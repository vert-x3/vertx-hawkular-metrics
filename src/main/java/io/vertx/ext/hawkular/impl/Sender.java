/*
 * Copyright 2015 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.hawkular.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;

/**
 * Sends collected metrics to the Hawkular server.
 *
 * @author Thomas Segismont
 */
public class Sender implements Handler<List<DataPoint>> {
  private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

  private static final CharSequence MEDIA_TYPE_APPLICATION_JSON = HttpHeaders.createOptimized("application/json");
  private static final CharSequence HTTP_HEADER_HAWKULAR_TENANT = HttpHeaders.createOptimized("Hawkular-Tenant");

  private static final Pattern HAWKULAR_VERSION = Pattern.compile("([0-9]+)\\.([0-9]+)\\.(.+)");

  private final Vertx vertx;
  private final String metricsServiceUri;

  private final CharSequence tenant;
  private final CharSequence auth;

  private final Map<CharSequence, Iterable<CharSequence>> httpHeaders;

  private final int batchSize;
  private final long batchDelay;
  private final List<DataPoint> queue;

  private HttpClient httpClient;
  private long timerId;

  private String metricsDataUri;

  private long sendTime;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public Sender(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.vertx = vertx;
    metricsServiceUri = options.getMetricsServiceUri();

    tenant = options.isSendTenantHeader() ? HttpHeaders.createOptimized(options.getTenant()) : null;
    AuthenticationOptions authenticationOptions = options.getAuthenticationOptions();
    if (authenticationOptions.isEnabled()) {
      String authString = authenticationOptions.getId() + ":" + authenticationOptions.getSecret();
      try {
        auth = HttpHeaders.createOptimized("Basic " + Base64.getEncoder().encodeToString(authString.getBytes("UTF-8")));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      auth = null;
    }

    JsonObject httpHeaders = options.getHttpHeaders();
    if (httpHeaders != null) {
      this.httpHeaders = new HashMap<>(httpHeaders.size());
      for (String headerName : httpHeaders.fieldNames()) {
        CharSequence optimizedName = HttpHeaders.createOptimized(headerName);
        Object value = httpHeaders.getValue(headerName);
        List<String> values;
        if (value instanceof JsonArray) {
          values = ((JsonArray) value).stream().map(Object::toString).collect(toList());
        } else {
          values = Collections.singletonList(value.toString());
        }
        this.httpHeaders.put(optimizedName, values.stream().map(HttpHeaders::createOptimized).collect(toList()));
      }
    } else {
      this.httpHeaders = Collections.emptyMap();
    }

    batchSize = options.getBatchSize();
    batchDelay = NANOSECONDS.convert(options.getBatchDelay(), SECONDS);
    queue = new ArrayList<>(batchSize);
    context.runOnContext(aVoid -> {
      HttpClientOptions httpClientOptions = options.getHttpOptions()
        .setDefaultHost(options.getHost())
        .setDefaultPort(options.getPort());
      httpClient = vertx.createHttpClient(httpClientOptions);
      timerId = vertx.setPeriodic(MILLISECONDS.convert(batchDelay, NANOSECONDS), this::flushIfIdle);
      }
    );
    sendTime = System.nanoTime();
  }

  @Override
  public void handle(List<DataPoint> dataPoints) {
    if (LOG.isTraceEnabled()) {
      String lineSeparator = System.getProperty("line.separator");
      String msg = "Handling data points: " + lineSeparator +
        dataPoints.stream().map(DataPoint::toString).collect(joining(lineSeparator));
      LOG.trace(msg);
    }

    if (queue.size() + dataPoints.size() < batchSize) {
      queue.addAll(dataPoints);
      return;
    }
    List<DataPoint> temp = new ArrayList<>(queue.size() + dataPoints.size());
    temp.addAll(queue);
    temp.addAll(dataPoints);
    queue.clear();
    do {
      List<DataPoint> subList = temp.subList(0, batchSize);
      send(subList);
      subList.clear();
    } while (temp.size() >= batchSize);
    queue.addAll(temp);
  }

  private void send(List<DataPoint> dataPoints) {
    String mixedData = toHawkularMixedData(dataPoints).encode();
    getMetricsDataUri(ar -> {
      if (ar.succeeded()) {
        HttpClientRequest request = httpClient.post(ar.result(), this::onResponse)
          .exceptionHandler(err -> LOG.trace("Could not send metrics", err))
          .putHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON);

        if (tenant != null) {
          request.putHeader(HTTP_HEADER_HAWKULAR_TENANT, tenant);
        }
        if (auth != null) {
          request.putHeader(HttpHeaders.AUTHORIZATION, auth);
        }
        httpHeaders.forEach(request::putHeader);

        request.end(mixedData, "UTF-8");
        sendTime = System.nanoTime();
      }
    });
  }

  private void getMetricsDataUri(Handler<AsyncResult<String>> handler) {
    if (metricsDataUri != null) {
      handler.handle(Future.succeededFuture(metricsDataUri));
      return;
    }
    httpClient.get(metricsServiceUri + "/status", statusResponse -> {
      statusResponse.bodyHandler(buffer -> {
        String hawkularVersion = new JsonObject(buffer).getString("Implementation-Version");
        if (hawkularVersion == null) {
          handler.handle(Future.failedFuture("No version info in status data"));
        } else {
          Matcher matcher = HAWKULAR_VERSION.matcher(hawkularVersion);
          if (!matcher.matches()) {
            handler.handle(Future.failedFuture("Cannot parse version " + hawkularVersion));
          } else if ("0".equals(matcher.group(1))) {
            String minor = matcher.group(2);
            if (minor.length() <= 2 && Integer.parseInt(minor) < 15) {
              metricsDataUri = metricsServiceUri + "/metrics/data";
            } else {
              metricsDataUri = metricsServiceUri + "/metrics/raw";
            }
            handler.handle(Future.succeededFuture(metricsDataUri));
          }
        }
      }).exceptionHandler(err -> handler.handle(Future.failedFuture(err)));
    }).exceptionHandler(err -> handler.handle(Future.failedFuture(err)))
      .putHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON).end();
  }

  private JsonObject toHawkularMixedData(List<DataPoint> dataPoints) {
    Map<? extends Class<? extends DataPoint>, Map<String, List<DataPoint>>> mixedData;
    mixedData = dataPoints.stream().collect(groupingBy(DataPoint::getClass, groupingBy(DataPoint::getName)));
    JsonObject json = new JsonObject();
    addMixedData(json, "gauges", mixedData.get(GaugePoint.class));
    addMixedData(json, "counters", mixedData.get(CounterPoint.class));
    return json;
  }

  private void addMixedData(JsonObject json, String type, Map<String, List<DataPoint>> data) {
    if (data == null) {
      return;
    }
    JsonArray metrics = new JsonArray();
    data.forEach((id, points) -> {
      JsonArray jsonDataPoints = points.stream()
        .map(this::toJsonDataPoint)
        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
      metrics.add(new JsonObject().put("id", id).put("data", jsonDataPoints));
    });
    json.put(type, metrics);
  }

  private JsonObject toJsonDataPoint(DataPoint dataPoint) {
    return new JsonObject().put("timestamp", dataPoint.getTimestamp()).put("value", dataPoint.getValue());
  }

  private void onResponse(HttpClientResponse response) {
    if (response.statusCode() != 200 && LOG.isTraceEnabled()) {
      response.bodyHandler(msg -> {
        LOG.trace("Could not send metrics: " + response.statusCode() + " : " + msg.toString());
      });
    }
  }

  private void flushIfIdle(Long timerId) {
    if (System.nanoTime() - sendTime > batchDelay && !queue.isEmpty()) {
      send(queue);
      queue.clear();
    }
  }

  public void stop() {
    vertx.cancelTimer(timerId);
    httpClient.close();
  }
}

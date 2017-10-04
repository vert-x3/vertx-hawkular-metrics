/*
 * Copyright (c) 2011-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.hawkular.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;
import io.vertx.ext.metrics.collector.impl.AvailabilityPoint;
import io.vertx.ext.metrics.collector.impl.CounterPoint;
import io.vertx.ext.metrics.collector.impl.DataPoint;
import io.vertx.ext.metrics.collector.impl.GaugePoint;
import io.vertx.ext.metrics.collector.impl.HttpReporterBase;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.*;

/**
 * Sends collected metrics to the Hawkular server.
 *
 * @author Thomas Segismont
 */
public class HawkularReporter extends HttpReporterBase<VertxHawkularOptions> {
  private static final Logger LOG = LoggerFactory.getLogger(HawkularReporter.class);

  private static final CharSequence MEDIA_TYPE_APPLICATION_JSON = HttpHeaders.createOptimized("application/json");
  private static final CharSequence HTTP_HEADER_HAWKULAR_TENANT = HttpHeaders.createOptimized("Hawkular-Tenant");

  private static final Pattern HAWKULAR_VERSION = Pattern.compile("([0-9]+)\\.([0-9]+)\\.(.+)");

  private final String metricsServiceUri;

  private final CharSequence tenant;
  private final CharSequence auth;

  private final JsonObject tags;
  private final List<MetricTagsMatcher> metricTagsMatchers;
  private final TaggedMetricsCache taggedMetricsCache;

  private String metricsDataUri;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public HawkularReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    super(vertx, options, context, options.getHttpHeaders());

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

    tags = options.getTags();
    metricTagsMatchers = options.getMetricTagsMatches().stream()
      .map(MetricTagsMatcher::new)
      .collect(toList());
    taggedMetricsCache = new TaggedMetricsCache(options.getTaggedMetricsCacheSize());

    onContextInit(options);
  }

  @Override
  protected HttpClientOptions createHttpClientOptions(VertxHawkularOptions options) {
    return options.getHttpOptions()
      .setDefaultHost(options.getHost())
      .setDefaultPort(options.getPort());
  }

  @Override
  protected void getMetricsDataUri(Handler<AsyncResult<String>> handler) {
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

  @Override
  protected void doSend(String metricsDataUri, List<DataPoint> dataPoints) {
    JsonObject mixedData = toMixedData(dataPoints);

    HttpClientRequest request = httpClient.post(metricsDataUri, this::onResponse)
      .exceptionHandler(t -> handleException(mixedData, t))
      .putHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON);

    if (tenant != null) {
      request.putHeader(HTTP_HEADER_HAWKULAR_TENANT, tenant);
    }
    if (auth != null) {
      request.putHeader(HttpHeaders.AUTHORIZATION, auth);
    }
    httpHeaders.forEach(request::putHeader);

    request.end(mixedData.toBuffer());

    tagMetrics(mixedData.getJsonArray("gauges"), "gauges");
    tagMetrics(mixedData.getJsonArray("counters"), "counters");
    tagMetrics(mixedData.getJsonArray("availabilities"), "availability");
  }

  private JsonObject toMixedData(List<DataPoint> dataPoints) {
    Map<? extends Class<? extends DataPoint>, Map<String, List<DataPoint>>> mixedData;
    mixedData = dataPoints.stream().collect(groupingBy(DataPoint::getClass, groupingBy(DataPoint::getName)));

    JsonObject json = new JsonObject();
    addMixedData(json, "gauges", mixedData.get(GaugePoint.class));
    addMixedData(json, "counters", mixedData.get(CounterPoint.class));
    addMixedData(json, "availabilities", mixedData.get(AvailabilityPoint.class));

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

  private void tagMetrics(JsonArray metrics, String type) {
    if (metrics == null) {
      return;
    }
    metrics.stream()
      .map(JsonObject.class::cast)
      .map(m -> m.getString("id"))
      .filter(name -> !taggedMetricsCache.isMetricTagged(type, name))
      .forEach(name -> {
        JsonObject json = new JsonObject();
        json.mergeIn(tags);
        metricTagsMatchers.forEach(matcher -> {
          if (matcher.matches(name)) {
            json.mergeIn(matcher.getTags());
          }
        });
        if (json.isEmpty()) {
          return;
        }
        try {
          String uri = metricsServiceUri + "/" + type + "/" + URLEncoder.encode(name, "UTF-8") + "/tags";
          HttpClientRequest request = httpClient.put(uri)
            .handler(response -> {
              if (response.statusCode() == 200) {
                taggedMetricsCache.metricTagged(type, name);
              } else if (LOG.isTraceEnabled()) {
                response.bodyHandler(msg -> {
                  LOG.trace("Could not send data: " + response.statusCode() + " : " + msg.toString());
                });
              }
            })
            .exceptionHandler(err -> LOG.trace("Could not send data", err))
            .putHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON);

          if (tenant != null) {
            request.putHeader(HTTP_HEADER_HAWKULAR_TENANT, tenant);
          }
          if (auth != null) {
            request.putHeader(HttpHeaders.AUTHORIZATION, auth);
          }
          httpHeaders.forEach(request::putHeader);

          request.end(json.toBuffer());
        } catch (UnsupportedEncodingException e) {
          LOG.trace("Could not encode metric name", e);
        }
      });
  }
}

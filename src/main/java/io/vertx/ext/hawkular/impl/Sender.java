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

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.hawkular.VertxHawkularOptions;
import org.hawkular.metrics.client.common.MetricType;
import org.hawkular.metrics.client.common.SingleMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;
import static org.hawkular.metrics.client.common.MetricType.*;

/**
 * Sends collected metrics to the Hawkular server.
 *
 * @author Thomas Segismont
 */
public class Sender implements Handler<List<SingleMetric>> {
  private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

  private final Vertx vertx;
  private final String metricsURI;
  private final String tenant;
  private final int batchSize;
  private final long batchDelay;
  private final List<SingleMetric> queue;

  private HttpClient httpClient;
  private long timerId;

  private long sendTime;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public Sender(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.vertx = vertx;
    metricsURI = options.getMetricsServiceUri() + "/metrics/data";
    tenant = options.getTenant();
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
  public void handle(List<SingleMetric> metrics) {
    if (queue.size() + metrics.size() < batchSize) {
      queue.addAll(metrics);
      return;
    }
    List<SingleMetric> temp = new ArrayList<>(queue.size() + metrics.size());
    temp.addAll(queue);
    temp.addAll(metrics);
    queue.clear();
    do {
      List<SingleMetric> subList = temp.subList(0, batchSize);
      send(subList);
      subList.clear();
    } while (temp.size() >= batchSize);
    queue.addAll(temp);
  }

  private void send(List<SingleMetric> metrics) {
    JsonObject mixedData = toHawkularMixedData(metrics);
    httpClient.post(metricsURI, this::onResponse)
      .putHeader("Content-Type", "application/json")
      .putHeader("Hawkular-Tenant", tenant)
      .exceptionHandler(err -> LOG.trace("Could not send metrics", err))
      .end(mixedData.encode(), "UTF-8");
    sendTime = System.nanoTime();
  }

  private JsonObject toHawkularMixedData(List<SingleMetric> metrics) {
    JsonObject mixedData = new JsonObject();
    Map<MetricType, List<SingleMetric>> map = metrics.stream().collect(groupingBy(singleMetric -> {
      // Hawkular only knows counters and gauges for now
      if (singleMetric.getMetricType() == COUNTER) {
        return COUNTER;
      }
      return GAUGE;
    }));
    List<SingleMetric> counterMetrics = map.get(COUNTER);
    if (counterMetrics != null) {
      // For now, gauges and counters are handled the same on the Vert.x side (Double value).
      // But this is going to change
      JsonArray counters = toHawkularGauges(counterMetrics);
      mixedData.put("counters", counters);
    }
    List<SingleMetric> gaugeMetrics = map.get(GAUGE);
    if (gaugeMetrics != null) {
      JsonArray gauges = toHawkularGauges(gaugeMetrics);
      mixedData.put("gauges", gauges);
    }
    return mixedData;
  }

  private JsonArray toHawkularGauges(List<SingleMetric> metrics) {
    JsonArray gauges = new JsonArray();
    metrics.forEach(singleMetric -> {

      JsonObject point = new JsonObject();
      point.put("timestamp", singleMetric.getTimestamp());
      point.put("value", singleMetric.getValue());

      JsonObject counter = new JsonObject();
      counter.put("id", singleMetric.getSource());
      counter.put("data", new JsonArray(Collections.singletonList(point)));

      gauges.add(counter);
    });
    return gauges;
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

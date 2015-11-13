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

import io.vertx.core.net.SocketAddress;
import org.hawkular.metrics.client.common.MetricType;
import org.hawkular.metrics.client.common.SingleMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.hawkular.metrics.client.common.MetricType.*;

/**
 * Aggregates values from {@link HttpServerMetricsImpl} instances and exposes metrics for collection.
 *
 * @author Thomas Segismont
 */
public class HttpServerMetricsSupplier implements MetricSupplier {
  private final String baseName;
  private final Set<HttpServerMetricsImpl> metricsSet = new CopyOnWriteArraySet<>();

  public HttpServerMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.http.server.";
  }

  @Override
  public List<SingleMetric> collect() {
    long timestamp = System.currentTimeMillis();

    Map<SocketAddress, Long> processingTime = new HashMap<>();
    Map<SocketAddress, Long> requestCount = new HashMap<>();
    Map<SocketAddress, Long> requests = new HashMap<>();
    Map<SocketAddress, Long> httpConnections = new HashMap<>();
    Map<SocketAddress, Long> wsConnections = new HashMap<>();
    Map<SocketAddress, Long> bytesReceived = new HashMap<>();
    Map<SocketAddress, Long> bytesSent = new HashMap<>();
    Map<SocketAddress, Long> errorCount = new HashMap<>();

    for (HttpServerMetricsImpl httpServerMetrics : metricsSet) {
      SocketAddress serverAddress = httpServerMetrics.getServerAddress();
      merge(processingTime, serverAddress, httpServerMetrics.getProcessingTime());
      merge(requestCount, serverAddress, httpServerMetrics.getRequestCount());
      merge(requests, serverAddress, httpServerMetrics.getRequests());
      merge(httpConnections, serverAddress, httpServerMetrics.getHttpConnections());
      merge(wsConnections, serverAddress, httpServerMetrics.getWsConnections());
      merge(bytesReceived, serverAddress, httpServerMetrics.getBytesReceived());
      merge(bytesSent, serverAddress, httpServerMetrics.getBytesSent());
      merge(errorCount, serverAddress, httpServerMetrics.getErrorCount());
    }

    List<SingleMetric> res = new ArrayList<>();
    res.addAll(metrics("processingTime", timestamp, processingTime, COUNTER));
    res.addAll(metrics("requestCount", timestamp, requestCount, COUNTER));
    res.addAll(metrics("requests", timestamp, requests, GAUGE));
    res.addAll(metrics("httpConnections", timestamp, httpConnections, GAUGE));
    res.addAll(metrics("wsConnections", timestamp, wsConnections, GAUGE));
    res.addAll(metrics("bytesReceived", timestamp, bytesReceived, COUNTER));
    res.addAll(metrics("bytesSent", timestamp, bytesSent, COUNTER));
    res.addAll(metrics("errorCount", timestamp, errorCount, COUNTER));
    return res;
  }

  private void merge(Map<SocketAddress, Long> values, SocketAddress serverAddress, Long value) {
    values.merge(serverAddress, value, Long::sum);
  }

  private List<SingleMetric> metrics(String name, long timestamp, Map<SocketAddress, ? extends Number> values, MetricType type) {
    List<SingleMetric> res = new ArrayList<>(values.size());
    values.forEach((address, count) -> {
      String addressId = address.host() + ":" + address.port();
      res.add(metric(addressId + "." + name, timestamp, count, type));
    });
    return res;
  }

  private SingleMetric metric(String name, long timestamp, Number value, MetricType type) {
    return new SingleMetric(baseName + name, timestamp, value.doubleValue(), type);
  }

  public void register(HttpServerMetricsImpl httpServerMetrics) {
    metricsSet.add(httpServerMetrics);
  }

  public void unregister(HttpServerMetricsImpl httpServerMetrics) {
    metricsSet.remove(httpServerMetrics);
  }
}

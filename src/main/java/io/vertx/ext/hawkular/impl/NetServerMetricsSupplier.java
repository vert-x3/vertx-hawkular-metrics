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
 * Aggregates values from {@link NetServerMetricsImpl} instances and exposes metrics for collection.
 *
 * @author Thomas Segismont
 */
public class NetServerMetricsSupplier implements MetricSupplier {
  private final String baseName;
  private final Set<NetServerMetricsImpl> metricsSet = new CopyOnWriteArraySet<>();

  public NetServerMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.net.server.";
  }

  @Override
  public List<SingleMetric> collect() {
    long timestamp = System.currentTimeMillis();

    Map<SocketAddress, Long> connections = new HashMap<>();
    Map<SocketAddress, Long> bytesReceived = new HashMap<>();
    Map<SocketAddress, Long> bytesSent = new HashMap<>();
    Map<SocketAddress, Long> errorCount = new HashMap<>();

    for (NetServerMetricsImpl netServerMetrics : metricsSet) {
      SocketAddress serverAddress = netServerMetrics.getServerAddress();
      merge(connections, serverAddress, netServerMetrics.getConnections());
      merge(bytesReceived, serverAddress, netServerMetrics.getBytesReceived());
      merge(bytesSent, serverAddress, netServerMetrics.getBytesSent());
      merge(errorCount, serverAddress, netServerMetrics.getErrorCount());
    }

    List<SingleMetric> res = new ArrayList<>();
    res.addAll(metrics("connections", timestamp, connections, GAUGE));
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

  public void register(NetServerMetricsImpl netServerMetrics) {
    metricsSet.add(netServerMetrics);
  }

  public void unregister(NetServerMetricsImpl netServerMetrics) {
    metricsSet.remove(netServerMetrics);
  }
}

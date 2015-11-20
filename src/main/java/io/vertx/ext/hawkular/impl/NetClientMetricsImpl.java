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
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.ext.hawkular.impl.NetClientConnectionsMeasurements.Snapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.*;

/**
 * @author Thomas Segismont
 */
public class NetClientMetricsImpl implements TCPMetrics<Void> {
  private final ConcurrentMap<SocketAddress, NetClientConnectionsMeasurements> connectionsMeasurements = new ConcurrentHashMap<>(0);
  private final NetClientMetricsSupplier netClientMetricsSupplier;

  public NetClientMetricsImpl(NetClientMetricsSupplier netClientMetricsSupplier) {
    this.netClientMetricsSupplier = netClientMetricsSupplier;
    netClientMetricsSupplier.register(this);
  }

  @Override
  public Void connected(SocketAddress remoteAddress, String remoteName) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(remoteAddress);
    if (measurements == null) {
      measurements = connectionsMeasurements.computeIfAbsent(remoteAddress, address -> new NetClientConnectionsMeasurements());
    }
    measurements.incrementConnections();
    return null;
  }

  @Override
  public void disconnected(Void socketMetric, SocketAddress remoteAddress) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(remoteAddress);
    if (measurements != null) {
      measurements.decrementConnections();
    }
  }

  @Override
  public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(remoteAddress);
    if (measurements != null) {
      measurements.addBytesReceived(numberOfBytes);
    }
  }

  @Override
  public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(remoteAddress);
    if (measurements != null) {
      measurements.addBytesSent(numberOfBytes);
    }
  }

  @Override
  public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(remoteAddress);
    if (measurements != null) {
      measurements.incrementErrorCount();
    }
  }

  /**
   * @return a snapshot of measurements for each remote address
   */
  public Map<SocketAddress, Snapshot> getMeasurementsSnapshot() {
    return connectionsMeasurements.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().getSnapshot()));
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    netClientMetricsSupplier.unregister(this);
  }
}

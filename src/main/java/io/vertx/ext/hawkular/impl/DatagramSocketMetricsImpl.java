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
import io.vertx.core.spi.metrics.DatagramSocketMetrics;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import static java.util.stream.Collectors.*;

/**
 * Implementation of {@link DatagramSocketMetrics} which relays data to {@link DatagramSocketMetricsSupplier}.
 *
 * @author Thomas Segismont
 */
public class DatagramSocketMetricsImpl implements DatagramSocketMetrics {
  private final LongAdder bytesReceived = new LongAdder();
  private final ConcurrentMap<SocketAddress, LongAdder> bytesSent = new ConcurrentHashMap<>(0);
  private final LongAdder errors = new LongAdder();
  private final DatagramSocketMetricsSupplier datagramSocketMetricsSupplier;

  private SocketAddress localAddress;

  public DatagramSocketMetricsImpl(DatagramSocketMetricsSupplier datagramSocketMetricsSupplier) {
    this.datagramSocketMetricsSupplier = datagramSocketMetricsSupplier;
    datagramSocketMetricsSupplier.register(this);
  }

  @Override
  public void listening(SocketAddress localAddress) {
    this.localAddress = localAddress;
  }

  @Override
  public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    bytesReceived.add(numberOfBytes);
  }

  @Override
  public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    LongAdder counter = bytesSent.get(remoteAddress);
    if (counter == null) {
      counter = bytesSent.computeIfAbsent(remoteAddress, address -> new LongAdder());
    }
    counter.add(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
    errors.increment();
  }

  /**
   * @return the local {@link SocketAddress} for listening {@link io.vertx.core.datagram.DatagramSocket}, null otherwise
   */
  public SocketAddress getServerAddress() {
    return localAddress;
  }

  /**
   * @return the number of bytes received for listening {@link io.vertx.core.datagram.DatagramSocket}, 0 otherwise
   */
  public long getBytesReceived() {
    return bytesReceived.sum();
  }

  /**
   * @return bytes sent per remote {@link SocketAddress}
   */
  public Map<SocketAddress, Long> getBytesSent() {
    return bytesSent.entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().sum()));
  }

  public long getErrorCount() {
    return errors.sum();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    datagramSocketMetricsSupplier.unregister(this);
  }
}

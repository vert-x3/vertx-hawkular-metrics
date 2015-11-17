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
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.impl.DummyVertxMetrics;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Metris SPI implementation.
 *
 * @author Thomas Segismont
 */
public class VertxMetricsImpl extends DummyVertxMetrics {
  private final String prefix;
  private final HttpServerMetricsSupplier httpServerMetricsSupplier;
  private final HttpClientMetricsSupplier httpClientMetricsSupplier;
  private final NetServerMetricsSupplier netServerMetricsSupplier;
  private final NetClientMetricsSupplier netClientMetricsSupplier;
  private final DatagramSocketMetricsSupplier datagramSocketMetricsSupplier;

  private Sender sender;
  private Scheduler scheduler;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   */
  public VertxMetricsImpl(Vertx vertx, VertxHawkularOptions options) {
    prefix = options.getPrefix();
    httpServerMetricsSupplier = new HttpServerMetricsSupplier(prefix);
    httpClientMetricsSupplier = new HttpClientMetricsSupplier(prefix);
    netServerMetricsSupplier = new NetServerMetricsSupplier(prefix);
    netClientMetricsSupplier = new NetClientMetricsSupplier(prefix);
    datagramSocketMetricsSupplier = new DatagramSocketMetricsSupplier(prefix);
    Context context = vertx.getOrCreateContext();
    sender = new Sender(vertx, options, context);
    scheduler = new Scheduler(vertx, options, context, sender);
    scheduler.register(httpServerMetricsSupplier);
    scheduler.register(httpClientMetricsSupplier);
    scheduler.register(netServerMetricsSupplier);
    scheduler.register(netClientMetricsSupplier);
    scheduler.register(datagramSocketMetricsSupplier);
  }

  @Override
  public HttpServerMetrics<Long, Void, Void> createMetrics(HttpServer server, SocketAddress localAddress,
                                                           HttpServerOptions options) {
    return new HttpServerMetricsImpl(localAddress, httpServerMetricsSupplier);
  }

  @Override
  public HttpClientMetrics createMetrics(HttpClient client, HttpClientOptions options) {
    return new HttpClientMetricsImpl(httpClientMetricsSupplier);
  }

  @Override
  public TCPMetrics createMetrics(NetServer server, SocketAddress localAddress, NetServerOptions options) {
    return new NetServerMetricsImpl(localAddress, netServerMetricsSupplier);
  }

  @Override
  public TCPMetrics createMetrics(NetClient client, NetClientOptions options) {
    return new NetClientMetricsImpl(netClientMetricsSupplier);
  }

  @Override
  public DatagramSocketMetrics createMetrics(DatagramSocket socket, DatagramSocketOptions options) {
    return new DatagramSocketMetricsImpl(datagramSocketMetricsSupplier);
  }

  @Override
  public EventBusMetrics createMetrics(EventBus eventBus) {
    return new EventBusMetricsImpl(prefix, scheduler);
  }

  @Override
  public boolean isMetricsEnabled() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    scheduler.unregister(httpServerMetricsSupplier);
    scheduler.unregister(httpClientMetricsSupplier);
    scheduler.unregister(netServerMetricsSupplier);
    scheduler.unregister(netClientMetricsSupplier);
    scheduler.unregister(datagramSocketMetricsSupplier);
    if (scheduler != null) {
      scheduler.stop();
    }
    if (sender != null) {
      sender.stop();
    }
  }
}

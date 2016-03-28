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

import java.util.Collections;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
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
import io.vertx.ext.hawkular.MetricsTypeEnum;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Metrics SPI implementation.
 *
 * @author Thomas Segismont
 */
public class VertxMetricsImpl extends DummyVertxMetrics {
  private final Vertx vertx;
  private final VertxHawkularOptions options;
  private final HttpServerMetricsSupplier httpServerMetricsSupplier;
  private final HttpClientMetricsSupplier httpClientMetricsSupplier;
  private final NetServerMetricsSupplier netServerMetricsSupplier;
  private final NetClientMetricsSupplier netClientMetricsSupplier;
  private final DatagramSocketMetricsSupplier datagramSocketMetricsSupplier;
  private final EventBusMetricsImpl eventBusMetrics;

  private Sender sender;
  private Scheduler scheduler;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   */
  public VertxMetricsImpl(Vertx vertx, VertxHawkularOptions options) {
    this.vertx = vertx;
    this.options = options;
    String prefix = options.getPrefix();
    httpServerMetricsSupplier = new HttpServerMetricsSupplier(prefix);
    httpClientMetricsSupplier = new HttpClientMetricsSupplier(prefix);
    netServerMetricsSupplier = new NetServerMetricsSupplier(prefix);
    netClientMetricsSupplier = new NetClientMetricsSupplier(prefix);
    datagramSocketMetricsSupplier = new DatagramSocketMetricsSupplier(prefix);
    eventBusMetrics = new EventBusMetricsImpl(prefix);
  }

  @Override
  public HttpServerMetrics<Long, Void, Void> createMetrics(HttpServer server, SocketAddress localAddress, HttpServerOptions options) {
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
    return eventBusMetrics;
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
  public void eventBusInitialized(EventBus bus) {
    // Finish setup
    Context context = vertx.getOrCreateContext();
    sender = new Sender(vertx, options, context);
    scheduler = new Scheduler(vertx, options, context, sender);
    if(this.options.isMetricsTypeDisabled(MetricsTypeEnum.HTTP_SERVER_TYPE))
      scheduler.register(httpServerMetricsSupplier);
    if(this.options.isMetricsTypeDisabled(MetricsTypeEnum.HTTP_CLIENT_TYPE))
      scheduler.register(httpClientMetricsSupplier);
    if(this.options.isMetricsTypeDisabled(MetricsTypeEnum.NET_SERVER_TYPE))
      scheduler.register(netServerMetricsSupplier);
    if(this.options.isMetricsTypeDisabled(MetricsTypeEnum.NET_CLIENT_TYPE))
      scheduler.register(netClientMetricsSupplier);
    if(this.options.isMetricsTypeDisabled(MetricsTypeEnum.DATAGRAM_SOCKET_TYPE))
      scheduler.register(datagramSocketMetricsSupplier);
    if(this.options.isMetricsTypeDisabled(MetricsTypeEnum.EVENT_BUS_TYPE))
      scheduler.register(eventBusMetrics);

    //Configure the metrics bridge. It just transforms the received metrics (json) to a Single Metric to enqueue it.
    if (options.isMetricsBridgeEnabled() && options.getMetricsBridgeAddress() != null) {
      bus.consumer(options.getMetricsBridgeAddress(), message -> {
        // By spec, it is a json object.
        JsonObject json = (JsonObject) message.body();

        // id (source) and value has to be set.
        // `id` is used to be homogeneous with Hawkular (using `id` as series identifier).
        // the timestamp can have been set in the message using the 'timestamp' field. If not use 'now'
        // the type of metrics can have been set in the message using the 'type' field. It not use 'gauge'. Only
        // "counter" and "gauge" are supported.
        String type = json.getString("type", "");
        String name = json.getString("id");
        long timestamp = json.getLong("timestamp", System.currentTimeMillis());
        DataPoint dataPoint;
        if ("counter".equals(type)) {
          dataPoint = new CounterPoint(name, timestamp, json.getLong("value"));
        } else {
          dataPoint = new GaugePoint(name, timestamp, json.getDouble("value"));
        }
        sender.handle(Collections.singletonList(dataPoint));
      });
    }
  }

  @Override
  public void close() {
    scheduler.unregister(httpServerMetricsSupplier);
    scheduler.unregister(httpClientMetricsSupplier);
    scheduler.unregister(netServerMetricsSupplier);
    scheduler.unregister(netClientMetricsSupplier);
    scheduler.unregister(datagramSocketMetricsSupplier);
    scheduler.unregister(eventBusMetrics);
    scheduler.stop();
    sender.stop();
  }


}

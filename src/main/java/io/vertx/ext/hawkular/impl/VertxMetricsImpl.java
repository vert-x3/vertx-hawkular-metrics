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
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.ext.hawkular.MetricsType;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static io.vertx.ext.hawkular.MetricsType.*;

/**
 * Metrics SPI implementation.
 *
 * @author Thomas Segismont
 */
public class VertxMetricsImpl extends DummyVertxMetrics {
  private final Vertx vertx;
  private final VertxHawkularOptions options;
  private final Map<MetricsType, MetricSupplier> metricSuppliers;

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
    Map<MetricsType, MetricSupplier> supplierMap = new EnumMap<>(MetricsType.class);
    if (!options.isMetricsTypeDisabled(HTTP_SERVER)) {
      supplierMap.put(HTTP_SERVER, new HttpServerMetricsSupplier(prefix));
    }
    if (!options.isMetricsTypeDisabled(HTTP_CLIENT)) {
      supplierMap.put(HTTP_CLIENT, new HttpClientMetricsSupplier(prefix));
    }
    if (!options.isMetricsTypeDisabled(NET_SERVER)) {
      supplierMap.put(NET_SERVER, new NetServerMetricsSupplier(prefix));
    }
    if (!options.isMetricsTypeDisabled(NET_CLIENT)) {
      supplierMap.put(NET_CLIENT, new NetClientMetricsSupplier(prefix));
    }
    if (!options.isMetricsTypeDisabled(DATAGRAM_SOCKET)) {
      supplierMap.put(DATAGRAM_SOCKET, new DatagramSocketMetricsSupplier(prefix));
    }
    if (!options.isMetricsTypeDisabled(EVENT_BUS)) {
      supplierMap.put(EVENT_BUS, new EventBusMetricsImpl(prefix));
    }
    if (!options.isMetricsTypeDisabled(NAMED_POOLS)) {
      supplierMap.put(NAMED_POOLS, new NamedPoolMetricsSupplier(prefix));
    }
    metricSuppliers = Collections.unmodifiableMap(supplierMap);
  }

  @Override
  public HttpServerMetrics<Long, Void, Void> createMetrics(HttpServer server, SocketAddress localAddress, HttpServerOptions options) {
    HttpServerMetricsSupplier supplier = (HttpServerMetricsSupplier) metricSuppliers.get(HTTP_SERVER);
    return supplier != null ? new HttpServerMetricsImpl(localAddress, supplier) : super.createMetrics(server, localAddress, options);
  }

  @Override
  public HttpClientMetrics createMetrics(HttpClient client, HttpClientOptions options) {
    HttpClientMetricsSupplier supplier = (HttpClientMetricsSupplier) metricSuppliers.get(HTTP_CLIENT);
    return supplier != null ? new HttpClientMetricsImpl(supplier) : super.createMetrics(client, options);
  }

  @Override
  public TCPMetrics createMetrics(SocketAddress localAddress, NetServerOptions options) {
    NetServerMetricsSupplier supplier = (NetServerMetricsSupplier) metricSuppliers.get(NET_SERVER);
    return supplier != null ? new NetServerMetricsImpl(localAddress, supplier) : super.createMetrics(localAddress, options);
  }

  @Override
  public TCPMetrics createMetrics(NetClient client, NetClientOptions options) {
    NetClientMetricsSupplier supplier = (NetClientMetricsSupplier) metricSuppliers.get(NET_CLIENT);
    return supplier != null ? new NetClientMetricsImpl(supplier) : super.createMetrics(client, options);
  }

  @Override
  public DatagramSocketMetrics createMetrics(DatagramSocket socket, DatagramSocketOptions options) {
    DatagramSocketMetricsSupplier supplier = (DatagramSocketMetricsSupplier) metricSuppliers.get(DATAGRAM_SOCKET);
    return supplier != null ? new DatagramSocketMetricsImpl(supplier) : super.createMetrics(socket, options);
  }

  @Override
  public EventBusMetrics createMetrics(EventBus eventBus) {
    EventBusMetrics supplier = (EventBusMetrics) metricSuppliers.get(EVENT_BUS);
    return supplier != null ? supplier : super.createMetrics(eventBus);
  }

  @Override
  public <P> PoolMetrics<?> createMetrics(P pool, String poolType, String poolName, int maxPoolSize) {
    NamedPoolMetricsSupplier supplier = (NamedPoolMetricsSupplier) metricSuppliers.get(NAMED_POOLS);
    PoolMetrics<?> poolMetrics;
    if (supplier != null) {
      poolMetrics = new PoolMetricsImpl(supplier, poolType, poolName, maxPoolSize);
    } else {
      poolMetrics = super.createMetrics(pool, poolType, poolName, maxPoolSize);
    }
    return poolMetrics;
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
    metricSuppliers.values().forEach(scheduler::register);

    //Configure the metrics bridge. It just transforms the received metrics (json) to a Single Metric to enqueue it.
    if (options.isMetricsBridgeEnabled() && options.getMetricsBridgeAddress() != null) {
      context.runOnContext(v -> {
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
      });
    }
  }

  @Override
  public void close() {
    metricSuppliers.values().forEach(scheduler::unregister);
    scheduler.stop();
    sender.stop();
  }


}

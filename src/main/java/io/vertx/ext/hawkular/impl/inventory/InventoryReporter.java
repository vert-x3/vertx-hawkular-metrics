/*
 * Copyright 2016 Red Hat, Inc.
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
package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.HashSet;
import java.util.Set;

/**
 * Report inventory to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class InventoryReporter {

  private static final Logger LOG = LoggerFactory.getLogger(InventoryReporter.class);
  private final Context context;
  private final Vertx vertx;
  private final VertxHawkularOptions options;
  private HttpClient httpClient;
  private FeedReporter feedReporter;
  private RootResourceReporter rootResourceReporter;
  private EventbusResourceReporter eventbusResourceReporter;
  private HttpClientResourceReporter httpClientResourceReporter;
  private DatagramSocketResourceReporter datagramSocketResourceReporter;
  private NetClientResourceReporter netClientResourceReporter;
  private Set<EntityReporter> entityReporters;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public InventoryReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.context = context;
    this.options = options;
    this.vertx = vertx;
    entityReporters = new HashSet<>();
    context.runOnContext(aVoid -> {
      HttpClientOptions httpClientOptions = options.getHttpOptions()
        .setDefaultHost(options.getHost())
        .setDefaultPort(options.getPort());

      httpClient = vertx.createHttpClient(httpClientOptions);
      datagramSocketResourceReporter = new DatagramSocketResourceReporter(options, httpClient);
      feedReporter = new FeedReporter(options, httpClient);
      entityReporters.add(feedReporter);
      String type = vertx.isClustered()? "cluster" : "standalone";
      rootResourceReporter = new RootResourceReporter(options, httpClient, type);
      entityReporters.add(rootResourceReporter);
      eventbusResourceReporter = new EventbusResourceReporter(options, httpClient);
      httpClientResourceReporter = new HttpClientResourceReporter(options, httpClient);
      datagramSocketResourceReporter = new DatagramSocketResourceReporter(options, httpClient);
      netClientResourceReporter = new NetClientResourceReporter(options, httpClient);
      entityReporters.add(eventbusResourceReporter);
      entityReporters.add(httpClientResourceReporter);
      entityReporters.add(datagramSocketResourceReporter);
      entityReporters.add(netClientResourceReporter);
      LOG.info("Inventory Reporter inited");
    });
  }
  public void report() {
    context.runOnContext(aVoid -> {
      entityReporters.forEach(e -> {
        e.register();
      });
      Future<Void> reported = Future.future();
      EntityReporter.report(reported);
      reported.setHandler(ar -> {
        if (ar.succeeded()) {
          LOG.info("report successfully.");
        } else {
          LOG.error("report failed.");
        }
      });
    });
  }

  public void stop() {
    httpClient.close();
  }

  public void registerHttpServer(SocketAddress address) {
    new HttpServerResourceReporter(options, httpClient, address).register();
  }

  public void registerNetServer(SocketAddress address) {
    entityReporters.add(new NetServerResourceReporter(options, httpClient, address));
  }

  public void addHttpClientAddress(SocketAddress address) {
    context.runOnContext(aVoid -> {
      httpClientResourceReporter.addRemoteAddress(address);
    });
  }

  public void addDatagramSentAddress(SocketAddress address) {
    context.runOnContext(aVoid -> {
      datagramSocketResourceReporter.addSentAddress(address);
    });

  }

  public void addDatagramReceivedAddress(SocketAddress address) {
    context.runOnContext(aVoid -> {
      datagramSocketResourceReporter.addReceivedAddress(address);
    });
  }

  public void addNetClientRemoteAddress(SocketAddress address) {
    context.runOnContext(aVoid -> {
      netClientResourceReporter.addRemoteAddress(address);
    });
  }

  public void addEventbusRemoteAddress(String address) {
    context.runOnContext(aVoid -> {
      eventbusResourceReporter.addRemoteAddress(address);
    });
  }
}

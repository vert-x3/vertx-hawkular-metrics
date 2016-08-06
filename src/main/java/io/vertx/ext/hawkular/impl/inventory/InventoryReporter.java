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
  private EntityReporter feedReporter;
  private EntityReporter rootResourceReporter;
  private HttpClientResourceReporter httpClientResourceReporter;
  private DatagramSocketResourceReporter datagramSocketResourceReporter;
  private NetClientResourceReporter netClientResourceReporter;
  private Set<EntityReporter> subResourceReporters;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public InventoryReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.context = context;
    this.options = options;
    this.vertx = vertx;
    subResourceReporters = new HashSet<>();
    context.runOnContext(aVoid -> {
      HttpClientOptions httpClientOptions = options.getHttpOptions()
        .setDefaultHost(options.getHost())
        .setDefaultPort(options.getPort());

      httpClient = vertx.createHttpClient(httpClientOptions);
      datagramSocketResourceReporter = new DatagramSocketResourceReporter(options, httpClient);
      feedReporter = new FeedReporter(options, httpClient);
      String type = vertx.isClustered()? "cluster" : "standalone";
      rootResourceReporter = new RootResourceReporter(options, httpClient, type);
      subResourceReporters.add(new EventbusResourceReporter(options, httpClient));
      httpClientResourceReporter = new HttpClientResourceReporter(options, httpClient);
      subResourceReporters.add(httpClientResourceReporter);
      datagramSocketResourceReporter = new DatagramSocketResourceReporter(options, httpClient);
      subResourceReporters.add(datagramSocketResourceReporter);
      netClientResourceReporter = new NetClientResourceReporter(options, httpClient);
      subResourceReporters.add(netClientResourceReporter);
      LOG.info("Inventory Reporter inited");
    });
  }
  public void report() {
    context.runOnContext(aVoid -> {
      Future<Void> feedCreated = Future.future();
      Future<Void> rootResourceCreated = Future.future();
      feedReporter.createFeed(feedCreated);
      feedCreated.compose(aVoid1 -> {
        rootResourceReporter.report(rootResourceCreated);
      }, rootResourceCreated);
      rootResourceCreated.setHandler(ar -> {
        if (ar.succeeded()) {
          subResourceReporters.forEach(this::handle);
          subResourceReporters.clear();
        } else {
          LOG.error(ar.cause().getLocalizedMessage());
        }
      });
    });
  }

  public void stop() {
    httpClient.close();
  }

  public void registerHttpServer(SocketAddress address) {
    subResourceReporters.add(new HttpServerResourceReporter(options, httpClient, address));
  }

  public void registerNetServer(SocketAddress address) {
    subResourceReporters.add(new NetServerResourceReporter(options, httpClient, address));
  }

  private void handle(EntityReporter reporter) {
    Future<Void> fut = Future.future();
    reporter.report(fut);
    fut.setHandler(ar -> {
      if (ar.succeeded()) {
        LOG.info("DONE : " + reporter.toString());
      } else {
        // retry when any error occurs.
        LOG.error(ar.cause().getLocalizedMessage());
        handle(reporter);
      }
    });
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
}

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


import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;


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
  private List<EntityReporter> subResourceReporters;
  private long sendTime;
  private final long batchDelay;
  private long timerId;


  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public InventoryReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.context = context;
    this.options = options;
    this.vertx = vertx;
    context.runOnContext(aVoid -> {
      HttpClientOptions httpClientOptions = options.getHttpOptions()
        .setDefaultHost(options.getHost())
        .setDefaultPort(options.getPort());
      httpClient = vertx.createHttpClient(httpClientOptions);
      feedReporter = new FeedReporter(options, httpClient);
      rootResourceReporter = new RootResourceReporter(options, httpClient);
      subResourceReporters = new ArrayList<>();
      subResourceReporters.add(new EventbusResourceReporter(options, httpClient));
      subResourceReporters.add(new HttpClientResourceReporter(options, httpClient));
    });
    sendTime = System.nanoTime();
    batchDelay = NANOSECONDS.convert(options.getBatchDelay(), SECONDS);
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
        timerId = vertx.setPeriodic(MILLISECONDS.convert(batchDelay, NANOSECONDS), this::reportSubResources);
      });
    });
  }

  public void stop() {
    httpClient.close();
    vertx.cancelTimer(timerId);
  }

  public void registerHttpServer(SocketAddress address) {
    subResourceReporters.add(new HttpServerResourceReporter(options, httpClient, address));
  }

  private void reportSubResources(Long timerId) {
    if (System.nanoTime() - sendTime > batchDelay && !subResourceReporters.isEmpty()) {
      subResourceReporters.forEach(this::handle);
      subResourceReporters.clear();
    }
    sendTime = System.nanoTime();
  }

  private void handle(EntityReporter reporter) {
    Future<Void> fut = Future.future();
    reporter.report(fut);
    fut.setHandler(ar -> {
      if (ar.succeeded()) {
        LOG.info("DONE : " + reporter.toString());
      } else {
        LOG.error("FAIL : " + reporter.toString());
        // retry when any error occurs.
        handle(reporter);
      }
    });
  }
}

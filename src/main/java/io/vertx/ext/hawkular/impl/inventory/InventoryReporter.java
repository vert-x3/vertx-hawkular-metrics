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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

/**
 * Report inventory to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class InventoryReporter {
  private static final Logger LOG = LoggerFactory.getLogger(InventoryReporter.class);


  private final Vertx vertx;
  private final Context context;
  private HttpClient httpClient;
  private final VertxHawkularOptions options;
  private EntityReporter feedReporter;
  private EntityReporter rootResourceReporter;
  private List<EntityReporter> subResourceReporters;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public InventoryReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.vertx = vertx;
    this.context = context;
    this.options = options;
    context.runOnContext(aVoid -> {
      HttpClientOptions httpClientOptions = options.getHttpOptions()
        .setDefaultHost(options.getHost())
        .setDefaultPort(options.getPort());
      httpClient = vertx.createHttpClient(httpClientOptions);
      feedReporter = new FeedReporter(options, httpClient);
      rootResourceReporter = new RootResourceReporter(options, httpClient);
      subResourceReporters = new ArrayList<>();
      subResourceReporters.add(new EventbusResourceReporter(options, httpClient));
      subResourceReporters.add(new HttpServerResourceReporter(options, httpClient, new SocketAddressImpl(8080, "0.0.0.0")));
    });
  }
  public void report() {
    context.runOnContext(aVoid -> {
      Future<Void> feedCreated = Future.future();
      Future<Void> rootResourceCreated = Future.future();
      Future<Void> subResourcesCreated = Future.future();
      feedReporter.createFeed(feedCreated);
      feedCreated.compose(aVoid1 -> {
        rootResourceReporter.report(rootResourceCreated);
      }, rootResourceCreated);
      rootResourceCreated.compose(aVoid1 -> {
        List<Future> futureList = new ArrayList<>(subResourceReporters.size());
        futureList.add(Future.future());
        futureList.add(Future.future());
        for (int i = 0; i < subResourceReporters.size(); i++) {
          EntityReporter r = subResourceReporters.get(i);
          r.report(futureList.get(i));
        }
        CompositeFuture.all(futureList).setHandler(ar -> {
          if (ar.succeeded()) {
            subResourcesCreated.complete();
          } else {
            subResourcesCreated.fail(ar.cause());
          }
        });
      }, subResourcesCreated);
      subResourcesCreated.setHandler(ar -> {
        if (ar.succeeded()) {
          System.out.println("DONE");
        } else {
          System.err.println(ar.cause().getLocalizedMessage());
        }
      });
    });
  }
  public void stop() {
    httpClient.close();
  }
}

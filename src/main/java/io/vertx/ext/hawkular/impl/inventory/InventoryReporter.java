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
import io.vertx.core.eventbus.impl.codecs.JsonObjectMessageCodec;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

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
  private List<EntityReporter> subEntityReporters;

  private static final CharSequence MEDIA_TYPE_APPLICATION_JSON = HttpHeaders.createOptimized("application/json");
  private static final CharSequence HTTP_HEADER_HAWKULAR_TENANT = HttpHeaders.createOptimized("Hawkular-Tenant");

  private static Map<CharSequence, Iterable<CharSequence>> httpHeaders;
  private static CharSequence auth;
  protected static String tenant;
  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public InventoryReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.context = context;
    this.options = options;
    this.vertx = vertx;

    tenant = options.isSendTenantHeader() ? options.getTenant() : null;
    AuthenticationOptions authenticationOptions = options.getAuthenticationOptions();
    if (authenticationOptions.isEnabled()) {
      String authString = authenticationOptions.getId() + ":" + authenticationOptions.getSecret();
      try {
        auth = HttpHeaders.createOptimized("Basic " + Base64.getEncoder().encodeToString(authString.getBytes("UTF-8")));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      auth = null;
    }
    JsonObject httpHeaders = options.getHttpHeaders();
    if (httpHeaders != null) {
      this.httpHeaders = new HashMap<>(httpHeaders.size());
      for (String headerName : httpHeaders.fieldNames()) {
        CharSequence optimizedName = HttpHeaders.createOptimized(headerName);
        Object value = httpHeaders.getValue(headerName);
        List<String> values;
        if (value instanceof JsonArray) {
          values = ((JsonArray) value).stream().map(Object::toString).collect(toList());
        } else {
          values = Collections.singletonList(value.toString());
        }
        this.httpHeaders.put(optimizedName, values.stream().map(HttpHeaders::createOptimized).collect(toList()));
      }
    } else {
      this.httpHeaders = Collections.emptyMap();
    }

    context.runOnContext(aVoid -> {
      HttpClientOptions httpClientOptions = options.getHttpOptions()
        .setDefaultHost(options.getHost())
        .setDefaultPort(options.getPort());
      httpClient = vertx.createHttpClient(httpClientOptions);
      feedReporter = new FeedReporter(options);
      String type = vertx.isClustered()? "cluster" : "standalone";
      rootResourceReporter = new RootResourceReporter(options, httpClient, type);
      datagramSocketResourceReporter = new DatagramSocketResourceReporter(options);
      eventbusResourceReporter = new EventbusResourceReporter(options);
      httpClientResourceReporter = new HttpClientResourceReporter(options);
      datagramSocketResourceReporter = new DatagramSocketResourceReporter(options);
      netClientResourceReporter = new NetClientResourceReporter(options) ;
      subEntityReporters = new ArrayList<>();
      subEntityReporters.add(eventbusResourceReporter);
      subEntityReporters.add(httpClientResourceReporter);
      subEntityReporters.add(datagramSocketResourceReporter);
      subEntityReporters.add(netClientResourceReporter);
      LOG.info("Inventory Reporter inited");
    });
  }
  public void report() {
    context.runOnContext(aVoid -> {
      Future<Void> feedCreated = Future.future();
      Future<Void> rootCreated = Future.future();
      handle(feedCreated, feedReporter.buildPayload());
      feedCreated.compose(aVoid1 -> {
        handle(rootCreated, rootResourceReporter.buildPayload());
      }, rootCreated);
      rootCreated.setHandler(aVoid1 -> {
        subEntityReporters.forEach(r -> {
          handleWrapper(r);
        });
      });
    });
  }

  private void handleWrapper(EntityReporter reporter) {
    Future<Void> fut = Future.future();
    handle(fut, reporter.buildPayload());
    fut.setHandler(ar -> {
      if (ar.succeeded()) {
        LOG.info("DONE {0}", reporter.toString());
      } else {
        // retry when error occurs
        LOG.error("FAIL {0} {1}", reporter.toString(), ar.cause().getLocalizedMessage());
        handleWrapper(reporter);
      }
    });
  }

  private void handle(Future<Void> reported, JsonObject payload) {
    LOG.debug("handling {0}", payload.encode());
    HttpClientRequest request = httpClient.post(options.getInventoryServiceUri() + "/bulk", response -> {
      if (response.statusCode() == 201) {
        reported.complete();
      } else {
        reported.fail(response.statusCode() + " " + response.statusMessage());
      }
    });
    addHeaders(request);
    request.end(payload.encode());
  }

  private void addHeaders(HttpClientRequest request) {
    request.putHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON);

    if (tenant != null) {
      request.putHeader(HTTP_HEADER_HAWKULAR_TENANT, tenant);
    }
    if (auth != null) {
      request.putHeader(HttpHeaders.AUTHORIZATION, auth);
    }
    httpHeaders.entrySet().stream().forEach(httpHeader -> {
      request.putHeader(httpHeader.getKey(), httpHeader.getValue());
    });
  }

  public void stop() {
    httpClient.close();
  }

  public void registerHttpServer(SocketAddress address) {
    context.runOnContext(aVoid -> {
      subEntityReporters.add(new HttpServerResourceReporter(options, address));
    });
  }

  public void registerNetServer(SocketAddress address) {
    context.runOnContext(aVoid -> {
      subEntityReporters.add(new NetServerResourceReporter(options, address));
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

  public void addEventbusRemoteAddress(String address) {
    context.runOnContext(aVoid -> {
      eventbusResourceReporter.addRemoteAddress(address);
    });
  }
}

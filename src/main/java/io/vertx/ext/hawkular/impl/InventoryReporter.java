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
package io.vertx.ext.hawkular.impl;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;
import jnr.ffi.annotations.In;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;

/**
 * Report inventory to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class InventoryReporter {
  private static final Logger LOG = LoggerFactory.getLogger(InventoryReporter.class);

  private static final CharSequence MEDIA_TYPE_APPLICATION_JSON = HttpHeaders.createOptimized("application/json");
  private static final CharSequence HTTP_HEADER_HAWKULAR_TENANT = HttpHeaders.createOptimized("Hawkular-Tenant");

  private final Vertx vertx;
  private final String inventoryURI;

  private final CharSequence tenant;
  private final CharSequence auth;

  private final Map<CharSequence, Iterable<CharSequence>> httpHeaders;

  private HttpClient httpClient;

  private final String feedId;
  private final String vertxRootResourceTypeId = "vertx-root";
  private final String vertxRootResourceId;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public InventoryReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.vertx = vertx;
    inventoryURI = options.getMetricsServiceUri() + "/inventory/deprecated";

    feedId = options.getFeedId();
    vertxRootResourceId = options.getVertxRootResourceId();

    tenant = options.isSendTenantHeader() ? HttpHeaders.createOptimized(options.getTenant()) : null;
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
            }
    );
  }

  public Future<HttpClientResponse> reportFeed() {
    Future<HttpClientResponse> fut = Future.future();
    httpClient.post(inventoryURI+"/feeds", response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create feed.");
      }
    }).end(new JsonObject().put("id", feedId).encode());
    return fut;
  }

  public Future<HttpClientResponse> createVertxRootResourceType() {
    Future<HttpClientResponse> fut = Future.future();
    httpClient.post(inventoryURI+"/feeds/"+feedId+"/resourceTypes", response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create vertx root resource type.");
      }
    }).end(new JsonObject().put("id", vertxRootResourceTypeId).encode());
    return fut;
  }

  public Future<HttpClientResponse> createVertxRootResource() {
    Future<HttpClientResponse> fut = Future.future();
    JsonObject json = new JsonObject().put("id", vertxRootResourceId)
            .put("resourceTypePath", "/f;" + feedId + "/rt;" + vertxRootResourceTypeId)
            .put("properties", new JsonObject().put("type", "standalone"));
    httpClient.post(inventoryURI+"/feeds/"+feedId+"/resources", response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create root resource.");
      }
    }).end(json.encode());
    return fut;
  }

  public void stop() {
    httpClient.close();
  }
}

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
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.io.UnsupportedEncodingException;
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

  private static final CharSequence MEDIA_TYPE_APPLICATION_JSON = HttpHeaders.createOptimized("application/json");
  private static final CharSequence HTTP_HEADER_HAWKULAR_TENANT = HttpHeaders.createOptimized("Hawkular-Tenant");

  private final Vertx vertx;
  private final String inventoryURI;

  private final CharSequence tenant;
  private final CharSequence auth;

  private final Map<CharSequence, Iterable<CharSequence>> httpHeaders;

  private HttpClient httpClient;

  private final String feedId;
  private final String vertxRootResourceTypeId = "rt.vertx-root";
  private final String eventbusResourceTypeId = "rt.eventbus";
  private final String vertxRootResourceId;
  private final String eventbusResourceId;

  private final String gaugeMetricTypeId = "mt.gauge";
  private final String eventbusMetricId = "m.handlers";

  private final int collectionInterval;
  private final VertxHawkularOptions options;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   */
  public InventoryReporter(Vertx vertx, VertxHawkularOptions options, Context context) {
    this.vertx = vertx;
    this.options = options;
    feedId = options.getFeedId();
    inventoryURI = options.getInventoryServiceUri();
    vertxRootResourceId = options.getVertxRootResourceId();
    eventbusResourceId = vertxRootResourceId + ".eventbus";
    collectionInterval = options.getSchedule();

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
    HttpClientRequest request = httpClient.post(composeEntityUri("", "feed"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create feed.");
      }
    });
    addHeaders(request);
    request.end(new JsonObject().put("id", feedId).encode());
    return fut;
  }

  public Future<HttpClientResponse> createVertxRootResourceType() {
    Future<HttpClientResponse> fut = Future.future();
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "resourceType"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create vertx root resource type.");
      }
    });
    addHeaders(request);
    request.end(new JsonObject().put("id", vertxRootResourceTypeId).encode());
    return fut;
  }

  public Future<HttpClientResponse> createVertxRootResource() {
    Future<HttpClientResponse> fut = Future.future();
    JsonObject json = new JsonObject().put("id", vertxRootResourceId)
            .put("resourceTypePath", "/f;" + feedId + "/rt;" + vertxRootResourceTypeId)
            .put("properties", new JsonObject().put("type", "standalone"));
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "resource"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create root resource.");
      }
    });
    addHeaders(request);
    request.end(json.encode());
    return fut;
  }

  public Future<HttpClientResponse> createEventbusResourceType() {
    Future<HttpClientResponse> fut = Future.future();
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "resourceType"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create event bus resource type.");
      }
    });
    addHeaders(request);
    request.end(new JsonObject().put("id", eventbusResourceTypeId).encode());
    return fut;
  }

  public Future<HttpClientResponse> createEventbusResource() {
    Future<HttpClientResponse> fut = Future.future();
    JsonObject json = new JsonObject().put("id", eventbusResourceId)
            .put("resourceTypePath", "/f;" + feedId + "/rt;" + eventbusResourceTypeId);
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId+"/r;"+vertxRootResourceId, "resource"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create event bus resource.");
      }
    });
    addHeaders(request);
    request.end(json.encode());
    return fut;
  }

  public Future<HttpClientResponse> createGaugeMetricType() {
    Future<HttpClientResponse> fut = Future.future();
    JsonObject json = new JsonObject().put("id", gaugeMetricTypeId)
            .put("type", "GAUGE")
            .put("unit", "NONE")
            .put("collectionInterval", collectionInterval);
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "metricType"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to create counter metric type.");
      }
    });
    addHeaders(request);
    request.end(json.encode());
    return fut;
  }

  public Future<HttpClientResponse> associateGaugeMetricTypeWithEventbusResourceType() {
    Future<HttpClientResponse> fut = Future.future();
    String metricPath = String.format("/t;%s/f;%s/mt;%s", tenant, feedId, gaugeMetricTypeId);
    JsonArray json = new JsonArray().add(metricPath);
    // This uses deprecated api because haven't find how to do this in new api.
    HttpClientRequest request = httpClient.post(inventoryURI+"/deprecated/feeds/"+feedId+"/resourceTypes/"+eventbusResourceTypeId+"/metricTypes", response -> {
      if (response.statusCode() == 204) {
        fut.complete(response);
      } else {
        fut.fail("Fail to associate counter metric type with event bus resource type");
      }
    });
    addHeaders(request);
    request.end(json.encode());
    return fut;
  }

  public Future<HttpClientResponse> createEventbusHandlerMetric() {
    Future<HttpClientResponse> fut = Future.future();
    String baseName = options.getPrefix() + (options.getPrefix().isEmpty() ? "" : ".") + "vertx.eventbus.";
    JsonObject json = new JsonObject().put("id", eventbusMetricId)
            .put("metricTypePath", "/f;" + feedId + "/mt;" + gaugeMetricTypeId)
            .put("properties", new JsonObject().put("metric-id", baseName+"handlers"));
    String path = String.format("f;%s/r;%s/r;%s", feedId, vertxRootResourceId, eventbusResourceId);
    HttpClientRequest request = httpClient.post(composeEntityUri(path, "metric"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        fut.fail("Fail to associate counter metric type with event bus resource type");
      }
    });
    addHeaders(request);
    request.end(json.encode());
    return fut;
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

  private String composeEntityUri(String path,String type) {
    if (!path.isEmpty()) {
      return String.format("%s/entity/%s/%s", inventoryURI, path, type);
    } else {
      return String.format("%s/entity/%s", inventoryURI, type);
    }
  }
}

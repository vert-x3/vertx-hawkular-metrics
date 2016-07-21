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
  private final Context context;
  private final String inventoryURI;

  private final CharSequence tenant;
  private final CharSequence auth;

  private final Map<CharSequence, Iterable<CharSequence>> httpHeaders;

  private HttpClient httpClient;

  private final String feedId;
  private final String metricBasename;
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
    this.context = context;
    this.options = options;
    feedId = options.getFeedId();
    metricBasename = options.getPrefix() + (options.getPrefix().isEmpty() ? "" : ".") + "vertx.";
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
  public void report() {
    context.runOnContext(aVoid -> {
      reportFeed().setHandler(ar -> {
        if (ar.succeeded()) {
          reportRootResource().setHandler(ar1 -> {
            if (ar1.succeeded()) {
              reportEventbusResource();
            } else {
              System.err.println(ar1.cause().getLocalizedMessage());
            }
          });
        } else {
          System.err.println(ar.cause().getLocalizedMessage());
        }
      });
    });
  }

  private Future<Void> reportFeed() {
    Future fut = Future.future();
    HttpClientRequest request = httpClient.post(composeEntityUri("", "feed"), response -> {
      if (response.statusCode() == 201) {
        fut.complete();
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create feed.");
      }
    });
    addHeaders(request);
    request.end(new JsonObject().put("id", feedId).encode());
    return fut;
  }

  private Future<Void> reportRootResource() {
    Future fut1 = Future.future();
    Future fut2 = Future.future();
    createResourceType(new JsonObject().put("id", vertxRootResourceTypeId)).setHandler(ar -> {
      if (ar.succeeded()) {
        fut1.complete();
      } else {
        fut1.fail(ar.cause());
      }
    });
    fut1.compose(ar -> {
      createResource("f;"+feedId, new JsonObject()
        .put("id", vertxRootResourceId)
        .put("resourceTypePath", "/f;" + feedId + "/rt;" + vertxRootResourceTypeId)
        .put("properties", new JsonObject().put("type", "standalone"))
      ).setHandler(ar1 -> {
        if (ar1.succeeded()) {
          System.out.println("Done with root resource");
          fut2.complete();
        } else {
          System.err.println(ar1.cause().getLocalizedMessage());
          fut2.fail(ar1.cause());
        }
      });
    }, fut2);
    return fut2;
  }

  private void reportEventbusResource() {
    createResourceType(new JsonObject().put("id", eventbusResourceTypeId)).setHandler(ar4 -> {
      if (ar4.succeeded()) {
        createResource("f;"+feedId+"/r;"+vertxRootResourceId, new JsonObject()
                .put("id", eventbusResourceId)
                .put("resourceTypePath", "/f;" + feedId + "/rt;" + eventbusResourceTypeId)
        ).setHandler(ar5 -> {
          if (ar5.succeeded()) {
            createMetricType(new JsonObject().put("id", gaugeMetricTypeId)
                    .put("type", "GAUGE")
                    .put("unit", "NONE")
                    .put("collectionInterval", collectionInterval)
            ).setHandler(ar6 -> {
              if (ar6.succeeded()) {
                String path = String.format("f;%s/r;%s/r;%s", feedId, vertxRootResourceId, eventbusResourceId);
                createMetric(path, new JsonObject().put("id", eventbusMetricId)
                        .put("metricTypePath", "/f;" + feedId + "/mt;" + gaugeMetricTypeId)
                        .put("properties", new JsonObject().put("metric-id", metricBasename+"eventbus.handlers"))
                ).setHandler(ar7 -> {
                  if (ar7.succeeded()) {
                    associateMetricTypeWithResourceType(gaugeMetricTypeId, eventbusResourceTypeId).setHandler(ar8 -> {
                      if (ar8.succeeded()) {
                        System.out.println("Done with event bus handler");
                      } else {
                        System.err.println(ar8.cause().getLocalizedMessage());
                      }
                    });
                  } else {
                    System.err.println(ar7.cause().getLocalizedMessage());
                  }
                });
              } else {
                System.err.println(ar6.cause().getLocalizedMessage());
              }
            });
          } else {
            System.err.println(ar5.cause().getLocalizedMessage());
          }
        });
      } else {
        System.err.println(ar4.cause().getLocalizedMessage());
      }
    });
  }

  private Future<HttpClientResponse> createResourceType(JsonObject body) {
    Future<HttpClientResponse> fut = Future.future();
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "resourceType"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create resource type with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
    return fut;
  }

  private Future<HttpClientResponse> createResource(String path, JsonObject body) {
    Future<HttpClientResponse> fut = Future.future();
    HttpClientRequest request = httpClient.post(composeEntityUri(path, "resource"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create resource with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
    return fut;
  }

  private Future<HttpClientResponse> createMetricType(JsonObject body) {
    Future<HttpClientResponse> fut = Future.future();
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "metricType"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create metric type with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
    return fut;
  }

  private Future<HttpClientResponse> createMetric(String path, JsonObject body) {
    Future<HttpClientResponse> fut = Future.future();
    HttpClientRequest request = httpClient.post(composeEntityUri(path, "metric"), response -> {
      if (response.statusCode() == 201) {
        fut.complete(response);
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create metric with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
    return fut;
  }

  private Future<HttpClientResponse> associateMetricTypeWithResourceType(String metricTypeId, String resourceTypeId) {
    Future<HttpClientResponse> fut = Future.future();
    String metricPath = String.format("/t;%s/f;%s/mt;%s", tenant, feedId, metricTypeId);
    JsonArray body = new JsonArray().add(metricPath);
    // This uses deprecated api because haven't find how to do this in new api.
    HttpClientRequest request = httpClient.post(inventoryURI+"/deprecated/feeds/"+feedId+"/resourceTypes/"+resourceTypeId+"/metricTypes", response -> {
      if (response.statusCode() == 204) {
        fut.complete(response);
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to associate metric type with resource type with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
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

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

import io.vertx.core.CompositeFuture;
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
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
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
  private final String httpServerResourceTypeId = "rt.http.server";
  private final String vertxRootResourceId;
  private final String eventbusResourceId;
  private final String httpServerResourceId;

  private final String gaugeMetricTypeId = "mt.gauge";
  private final String counterMetricTypeId = "mt.counter";
  private final String eventbusHandlerMetricId = "m.eventbus.handlers";
  private final String httpServerRequestCountMetricId = "m.http.server.requestCount";

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
    httpServerResourceId = vertxRootResourceId + ".http.server";

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
      Future<Void> fut1 = Future.future();
      Future<Void> fut2 = Future.future();
      Future<Void> fut3 = Future.future();
      reportFeed(fut1);
      fut1.compose(aVoid1 -> {
        reportRootResource(fut2);
      }, fut2);
      fut2.compose(aVoid1 -> {
        Future<Void> rfut1 = Future.future();
        Future<Void> rfut2 = Future.future();
        reportEventbusResource(rfut1);
        reportHttpServerResource(new SocketAddressImpl(8080, "0.0.0.0"), rfut2);
        CompositeFuture.all(rfut1, rfut2).setHandler(ar -> {
          if (ar.succeeded()) {
            fut3.complete();
          } else {
            fut3.fail(ar.cause());
          }
        });
      }, fut3);
      fut3.setHandler(ar -> {
        if (ar.succeeded()) {
          System.err.println("Done all reporting.");
        } else {
          System.err.println(ar.cause().getLocalizedMessage());
        }
      });
    });
  }

  private void reportFeed(Future<Void> fut) {
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
  }

  private void reportRootResource(Future<Void> fut) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    createResourceType(new JsonObject().put("id", vertxRootResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject()
              .put("id", vertxRootResourceId)
              .put("resourceTypePath", "/f;" + feedId + "/rt;" + vertxRootResourceTypeId)
              .put("properties", new JsonObject().put("type", "standalone"));
      createResource("f;"+feedId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      System.out.println("Done root resource reporting");
      fut.complete();
    }, fut);
  }

  private void reportEventbusResource(Future<Void> fut) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    Future<Void> fut3 = Future.future();
    Future<Void> fut4 = Future.future();
    Future<Void> fut5 = Future.future();

    createResourceType(new JsonObject().put("id", eventbusResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", eventbusResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + eventbusResourceTypeId);
      createResource("f;" + feedId + "/r;" + vertxRootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", gaugeMetricTypeId).put("type", "GAUGE").put("unit", "NONE").put("collectionInterval", collectionInterval);
      createMetricType( body, fut3);
    }, fut3);
    fut3.compose(aVoid -> {
      String path = String.format("f;%s/r;%s/r;%s", feedId, vertxRootResourceId, eventbusResourceId);
      JsonObject body = new JsonObject().put("id", eventbusHandlerMetricId).put("metricTypePath", "/f;" + feedId + "/mt;" + gaugeMetricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricBasename+"eventbus.handlers"));
      createMetric(path, body, fut4);
    }, fut4);
    fut4.compose(aVoid -> {
      associateMetricTypeWithResourceType(gaugeMetricTypeId, eventbusResourceTypeId, fut5);
    }, fut5);
    fut5.setHandler(ar -> {
      if (ar.succeeded()){
        fut.complete();
        System.out.println("Done event bus reporting");
      } else {
        fut.fail(ar.cause());
      }
    });
  }

  private void reportHttpServerResource(SocketAddress localAddress, Future<Void> fut) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    Future<Void> fut3 = Future.future();
    Future<Void> fut4 = Future.future();
    Future<Void> fut5 = Future.future();

    createResourceType(new JsonObject().put("id", httpServerResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", httpServerResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + httpServerResourceTypeId);
      createResource("f;" + feedId + "/r;" + vertxRootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", counterMetricTypeId).put("type", "COUNTER").put("unit", "NONE").put("collectionInterval", collectionInterval);
      createMetricType(body, fut3);
    }, fut3);
    fut3.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", httpServerRequestCountMetricId).put("metricTypePath", "/f;" + feedId + "/mt;" + counterMetricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricBasename+"http.server."+localAddress.host()+":"+localAddress.port()+".requestCount"));
      String path = String.format("f;%s/r;%s/r;%s", feedId, vertxRootResourceId, httpServerResourceId);
      createMetric(path, body,fut4);
    }, fut4);
    fut4.compose(aVoid -> {
      associateMetricTypeWithResourceType(counterMetricTypeId, httpServerResourceTypeId, fut5);
    }, fut5);
    fut5.setHandler(ar -> {
      if (ar.succeeded()) {
        fut.complete();
        System.out.println("Done http server reporting");
      } else {
        fut.fail(ar.cause());
      }
    });
  }

  private void createResourceType(JsonObject body, Future<Void> fut) {
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "resourceType"), response -> {
      if (response.statusCode() == 201) {
        fut.complete();
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create resource type with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
  }

  private void createResource(String path, JsonObject body, Future<Void> fut) {
    HttpClientRequest request = httpClient.post(composeEntityUri(path, "resource"), response -> {
      if (response.statusCode() == 201) {
        fut.complete();
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create resource with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
  }

  private void createMetricType(JsonObject body, Future<Void> fut) {
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "metricType"), response -> {
      if (response.statusCode() == 201) {
        fut.complete();
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create metric type with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
  }

  private void createMetric(String path, JsonObject body, Future<Void> fut) {
    HttpClientRequest request = httpClient.post(composeEntityUri(path, "metric"), response -> {
      if (response.statusCode() == 201) {
        fut.complete();
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to create metric with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
  }

  private void associateMetricTypeWithResourceType(String metricTypeId, String resourceTypeId, Future<Void> fut) {
    String metricPath = String.format("/t;%s/f;%s/mt;%s", tenant, feedId, metricTypeId);
    JsonArray body = new JsonArray().add(metricPath);
    // This uses deprecated api because haven't find how to do this in new api.
    HttpClientRequest request = httpClient.post(inventoryURI+"/deprecated/feeds/"+feedId+"/resourceTypes/"+resourceTypeId+"/metricTypes", response -> {
      if (response.statusCode() == 204) {
        fut.complete();
      } else {
        response.bodyHandler(buffer -> {
          System.err.println(buffer.getBuffer(0, buffer.length()));
        });
        fut.fail("Fail to associate metric type with resource type with payload : " + body.encode());
      }
    });
    addHeaders(request);
    request.end(body.encode());
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

package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Report a single entity to the Hawkular server.
 *
 * @author Austin Kuo
 */
public abstract class EntityReporter {

  protected static String feedId;
  protected static String metricBasename;
  protected static String inventoryURI;
  protected static String rootResourceTypeId = "rt.vertx-root";
  protected static String rootResourceId;
  protected static int collectionInterval;

  private static HttpClient httpClient;
  private static final CharSequence MEDIA_TYPE_APPLICATION_JSON = HttpHeaders.createOptimized("application/json");
  private static final CharSequence HTTP_HEADER_HAWKULAR_TENANT = HttpHeaders.createOptimized("Hawkular-Tenant");

  private static Map<CharSequence, Iterable<CharSequence>> httpHeaders;
  private static CharSequence tenant;
  private static CharSequence auth;

  EntityReporter(VertxHawkularOptions options, HttpClient httpClient) {
    feedId = options.getFeedId();
    metricBasename = options.getPrefix() + (options.getPrefix().isEmpty() ? "" : ".") + "vertx.";
    inventoryURI = options.getInventoryServiceUri();
    this.httpClient = httpClient;
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
    rootResourceId = options.getVertxRootResourceId();
    collectionInterval = options.getSchedule();
  }

  protected static void createFeed(Future<Void> fut) {
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

  protected static void createResourceType(JsonObject body, Future<Void> fut) {
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "resourceType"), response -> {
      if (response.statusCode() == 201 || response.statusCode() == 409) {
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

  protected static void createResource(String path, JsonObject body, Future<Void> fut) {
    HttpClientRequest request = httpClient.post(composeEntityUri(path, "resource"), response -> {
      if (response.statusCode() == 201 || response.statusCode() == 409) {
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

  protected static void createMetricType(JsonObject body, Future<Void> fut) {
    HttpClientRequest request = httpClient.post(composeEntityUri("f;"+feedId, "metricType"), response -> {
      if (response.statusCode() == 201 || response.statusCode() == 409) {
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

  protected static void createMetric(String path, JsonObject body, Future<Void> fut) {
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

  protected static void associateMetricTypeWithResourceType(String metricTypeId, String resourceTypeId, Future<Void> fut) {
    String metricPath = String.format("/t;%s/f;%s/mt;%s", tenant, feedId, metricTypeId);
    JsonArray body = new JsonArray().add(metricPath);
    // This uses deprecated api because haven't find how to do this in new api.
    HttpClientRequest request = httpClient.post(inventoryURI+"/deprecated/feeds/"+feedId+"/resourceTypes/"+resourceTypeId+"/metricTypes", response -> {
      if (response.statusCode() == 204 || response.statusCode() == 409) {
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

  private static void addHeaders(HttpClientRequest request) {
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

  private static String composeEntityUri(String path,String type) {
    if (!path.isEmpty()) {
      return String.format("%s/entity/%s/%s", inventoryURI, path, type);
    } else {
      return String.format("%s/entity/%s", inventoryURI, type);
    }
  }

  abstract void report(Future<Void> future);
}

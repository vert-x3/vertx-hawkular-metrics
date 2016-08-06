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
public class EntityReporter {

  protected static String feedId;
  protected static String tenant;
  protected static String metricBasename;
  protected static String inventoryURI;
  protected static String rootResourceTypeId = "rt.vertx-root";
  protected static String rootResourceId;
  protected static int collectionInterval;
  protected static String tenantPath;
  protected static String feedPath;
  protected static String rootResourcePath;

  protected static final String FEED = "feed";
  protected static final String RESOURCE = "resource";
  protected static final String RESOURCE_TYPE = "resourceType";
  protected static final String METRIC = "metric";
  protected static final String METRIC_TYPE = "metricType";



  private static HttpClient httpClient;
  private static final CharSequence MEDIA_TYPE_APPLICATION_JSON = HttpHeaders.createOptimized("application/json");
  private static final CharSequence HTTP_HEADER_HAWKULAR_TENANT = HttpHeaders.createOptimized("Hawkular-Tenant");

  private static Map<CharSequence, Iterable<CharSequence>> httpHeaders;
  private static CharSequence auth;
  private static JsonObject bulkJson;

  EntityReporter(VertxHawkularOptions options, HttpClient httpClient) {
    feedId = options.getFeedId();
    metricBasename = options.getPrefix() + (options.getPrefix().isEmpty() ? "" : ".") + "vertx.";
    inventoryURI = options.getInventoryServiceUri();
    this.httpClient = httpClient;
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
    rootResourceId = options.getVertxRootResourceId();
    collectionInterval = options.getSchedule();
    tenantPath = String.format("/t;%s", tenant);
    feedPath = String.format("/t;%s/f;%s", tenant, feedId);
    rootResourcePath = String.format("/t;%s/f;%s/r;%s", tenant, feedId, rootResourceId);
    bulkJson =  new JsonObject().put(tenantPath, new JsonObject().put("feed", new JsonArray()))
            .put(feedPath, new JsonObject()
                    .put("resource", new JsonArray())
                    .put("resourceType", new JsonArray())
                    .put("metricType", new JsonArray())
                    .put("relationship", new JsonArray()));
  }

  protected static void addEntity(String path, String type, JsonObject entity) {
    if (!bulkJson.containsKey(path)) {
      bulkJson.put(path, new JsonObject());
    }
    if (!bulkJson.getJsonObject(path).containsKey(type)) {
      bulkJson.getJsonObject(path).put(type, new JsonArray());
    }
    bulkJson.getJsonObject(path).getJsonArray(type).add(entity);
  }
  /*protected static void associateMetricTypeWithResourceType(String metricTypeId, String resourceTypeId, Future<Void> fut) {
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
  */

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

  protected void register() {

  };

  protected static void report(Future<Void> future) {
    HttpClientRequest request = httpClient.post(inventoryURI+"/bulk", response -> {
      response.bodyHandler(buffer -> {
        System.out.println(buffer.getBuffer(0, buffer.length()));
      });
      if (response.statusCode() == 201) {
        future.complete();
      } else {
        future.fail("Fail to report " + bulkJson.encode());
      }
    });
    addHeaders(request);
    System.out.println(bulkJson.encodePrettily());
    request.end(bulkJson.encode());
  };
}

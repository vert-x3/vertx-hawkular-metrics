package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report a http client resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class HttpClientResourceReporter extends EntityReporter {

  private final String httpClientResourceTypeId = "rt.http.client";
  private final String httpClientResourceId;

  HttpClientResourceReporter(VertxHawkularOptions options, HttpClient httpClient) {
    super(options, httpClient);
    httpClientResourceId = rootResourceId + ".http.client";
  }
  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    createResourceType(new JsonObject().put("id", httpClientResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", httpClientResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + httpClientResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        future.fail(ar.cause());
      }
    });
  }
}

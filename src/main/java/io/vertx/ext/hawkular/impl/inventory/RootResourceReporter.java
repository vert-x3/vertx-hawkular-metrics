package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report the root resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class RootResourceReporter extends EntityReporter {

  RootResourceReporter(VertxHawkularOptions options, HttpClient httpClient) {
    super(options, httpClient);
  }
  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    createResourceType(new JsonObject().put("id", rootResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject()
              .put("id", rootResourceId)
              .put("resourceTypePath", "/f;" + feedId + "/rt;" + rootResourceTypeId)
              .put("properties", new JsonObject().put("type", "standalone"));
      createResource("f;"+feedId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      future.complete();
    }, future);
  }
}

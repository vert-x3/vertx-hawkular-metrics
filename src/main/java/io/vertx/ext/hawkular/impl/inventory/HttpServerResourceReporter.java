package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report http server resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class HttpServerResourceReporter extends EntityReporter {

  private final SocketAddress localAddress;
  private final String httpServerResourceTypeId = "rt.http.server";
  private final String counterMetricTypeId = "mt.counter";
  private final String httpServerRequestCountMetricId = "m.http.server.requestCount";
  private final String httpServerResourceId;
  HttpServerResourceReporter(VertxHawkularOptions options, HttpClient httpClient, SocketAddress localAddress) {
    super(options, httpClient);
    this.localAddress = localAddress;
    httpServerResourceId = rootResourceId + ".http.server."+localAddress.host()+":"+localAddress.port();
  }

  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    Future<Void> fut3 = Future.future();
    Future<Void> fut4 = Future.future();
    Future<Void> fut5 = Future.future();

    createResourceType(new JsonObject().put("id", httpServerResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", httpServerResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + httpServerResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", counterMetricTypeId).put("type", "COUNTER").put("unit", "NONE").put("collectionInterval", collectionInterval);
      createMetricType(body, fut3);
    }, fut3);
    fut3.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", httpServerRequestCountMetricId).put("metricTypePath", "/f;" + feedId + "/mt;" + counterMetricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricBasename+"http.server."+localAddress.host()+":"+localAddress.port()+".requestCount"));
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, httpServerResourceId);
      createMetric(path, body,fut4);
    }, fut4);
    fut4.compose(aVoid -> {
      associateMetricTypeWithResourceType(counterMetricTypeId, httpServerResourceTypeId, fut5);
    }, fut5);
    fut5.setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        future.fail(ar.cause());
      }
    });
  }
}

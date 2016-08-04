package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Report http server resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class HttpServerResourceReporter extends EntityReporter {

  private final SocketAddress localAddress;
  private static final String httpServerResourceTypeId = "rt.http.server";

  private static final String requestCountMetricTypeId = "mt.counter.requestCount";
  private static final String processingTimeMetricTypeId = "mt.counter.processingTime";
  private static final String bytesReceivedMetricTypeId = "mt.counter.bytesReceived";
  private static final String bytesSentMetricTypeId = "mt.counter.bytesSent";
  private static final String errorCountMetricTypeId = "mt.counter.errorCount";
  private static final String requestsMetricTypeId = "mt.counter.requests";
  private static final String httpConnectionsMetricTypeId = "mt.counter.httpConnections";
  private static final String wsConnectionsMetricTypeId = "mt.counter.wsConnections";

  private static final String requestCountMetricId = "m.http.server.requestCount";
  private static final String processingTimeMetricId = "m.http.server.processingTime";
  private static final String bytesReceivedMetricId = "m.http.server.bytesReceived";
  private static final String bytesSentMetricId = "m.http.server.bytesSent";
  private static final String errorCountMetricId = "m.http.server.errorCount";
  private static final String requestsMetricId = "m.http.server.requests";
  private static final String httpConnectionsMetricId = "m.http.server.httpConnections";
  private static final String wsConnectionsMetricId = "m.http.server.wsConnections";

  private final String httpServerResourceId;
  private static final int numMetrics = 8;
  
  HttpServerResourceReporter(VertxHawkularOptions options, HttpClient httpClient, SocketAddress localAddress) {
    super(options, httpClient);
    this.localAddress = localAddress;
    httpServerResourceId = rootResourceId + ".http.server."+localAddress.host()+":"+localAddress.port();
  }

  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();

    createResourceType(new JsonObject().put("id", httpServerResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", httpServerResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + httpServerResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid1 -> {
      List<Future> futureList = new ArrayList(numMetrics);
      for (int i = 0; i < numMetrics; i++) {
        futureList.add(Future.future());
      }
      reportMetric(futureList.get(0), requestCountMetricTypeId, ".requestCount", "NONE", "COUNTER");
      reportMetric(futureList.get(1), processingTimeMetricTypeId, ".processingTime", "MILLISECONDS", "COUNTER");
      reportMetric(futureList.get(2), bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER");
      reportMetric(futureList.get(3), bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER");
      reportMetric(futureList.get(4), errorCountMetricTypeId, ".errorCount", "BYTES", "COUNTER");
      reportMetric(futureList.get(5), requestsMetricTypeId, ".requests", "NONE", "GAUGE");
      reportMetric(futureList.get(6), httpConnectionsMetricTypeId, ".httpConnections", "NONE", "GAUGE");
      reportMetric(futureList.get(7), wsConnectionsMetricTypeId, ".wsConnections", "NONE", "GAUGE");
      CompositeFuture.all(futureList).setHandler(ar -> {
        if (ar.succeeded()) {
          future.complete();
        } else {
          future.fail(ar.cause());
        }
      });
    }, future);
  }

  private void reportMetric(Future<Void> future, String metricTypeId, String postFix, String unit, String type) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    String metricId = metricBasename+"http.server."+localAddress.host()+":"+localAddress.port()+postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    createMetricType(body, fut1);
    fut1.compose(aVoid -> {
      JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricId));
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, httpServerResourceId);
      createMetric(path, body1,fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      associateMetricTypeWithResourceType(metricTypeId, httpServerResourceTypeId, future);
    }, future);
  }
}

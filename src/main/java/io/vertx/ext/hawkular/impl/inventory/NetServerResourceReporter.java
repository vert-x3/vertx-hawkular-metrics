package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;
import sun.nio.ch.Net;

import java.util.ArrayList;
import java.util.List;

/**
 * Report the client server resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class NetServerResourceReporter extends EntityReporter {

  private static final String netServerResourceTypeId = "rt.net.server";
  private static final String bytesReceivedMetricTypeId = "mt.counter.bytesReceived";
  private static final String bytesSentMetricTypeId = "mt.counter.bytesSent";
  private static final String errorCountMetricTypeId = "mt.counter.errorCount";
  private static final String connectionsMetricTypeId = "mt.counter.connections";
  private final String netServerResourceId;
  private final SocketAddress localAddress;
  private static int numMetrics = 4;

  NetServerResourceReporter(VertxHawkularOptions options, HttpClient httpClient, SocketAddress localAddress) {
    super(options, httpClient);
    this.localAddress = localAddress;
    netServerResourceId = rootResourceId + ".net.server."+localAddress.host()+":"+localAddress.port();
  }

  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();

    createResourceType(new JsonObject().put("id", netServerResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", netServerResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + netServerResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid1 -> {
      List<Future> futureList = new ArrayList(numMetrics);
      for (int i = 0; i < numMetrics; i++) {
        futureList.add(Future.future());
      }
      reportMetric(futureList.get(0), connectionsMetricTypeId, ".connections", "NONE", "GAUGE");
      reportMetric(futureList.get(1), errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER");
      reportMetric(futureList.get(2), bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER");
      reportMetric(futureList.get(3), bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER");
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
    String metricId = metricBasename+"net.server."+localAddress.host()+":"+localAddress.port()+postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    createMetricType(body, fut1);
    fut1.compose(aVoid -> {
      JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricId));
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, netServerResourceId);
      createMetric(path, body1,fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      associateMetricTypeWithResourceType(metricTypeId, netServerResourceTypeId, future);
    }, future);
  }
}

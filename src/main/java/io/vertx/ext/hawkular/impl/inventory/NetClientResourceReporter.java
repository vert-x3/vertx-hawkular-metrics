package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Report the net client resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class NetClientResourceReporter extends EntityReporter {

  private final Set<SocketAddress> remoteAddresses = new HashSet<>();
  private static final String netClientResourceTypeId = "rt.net.client";
  private final String netClientResourceId;
  private static final String connectionsMetricTypeId = "mt.gauge.connections";
  private static final String bytesReceivedMetricTypeId = "mt.counter.bytesReceived";
  private static final String bytesSentMetricTypeId = "mt.counter.bytesSent";
  private static final String errorCountMetricTypeId = "mt.counter.errorCount";
  private static final int numMetrics = 4;

  NetClientResourceReporter(VertxHawkularOptions options, HttpClient httpClient) {
    super(options, httpClient);
    netClientResourceId = rootResourceId + ".net.client";
  }
  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    createResourceType(new JsonObject().put("id", netClientResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", netClientResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + netClientResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      List<Future> futureList = new ArrayList<>();
      remoteAddresses.forEach(addr -> {
        Future fut = Future.future();
        futureList.add(fut);
        reportAddressMetric(addr, fut);
      });
      CompositeFuture.all(futureList).setHandler(ar -> {
        if (ar.succeeded()) {
          future.complete();
        } else {
          future.fail(ar.cause());
        }
      });
    }, future);
  }


  protected void addRemoteAddress(SocketAddress address) {
    remoteAddresses.add(address);
  }

  private void reportAddressMetric(SocketAddress address, Future<Void> future) {

    List<Future> futureList = new ArrayList<>(numMetrics);
    for (int i = 0; i < numMetrics; i++) {
      futureList.add(Future.future());
    }
    reportMetric(futureList.get(0), connectionsMetricTypeId, ".connections", "NONE", "GAUGE", address);
    reportMetric(futureList.get(1), bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER", address);
    reportMetric(futureList.get(2), bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER", address);
    reportMetric(futureList.get(3), errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER", address);
    CompositeFuture.all(futureList).setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        future.fail(ar.cause());
      }
    });
  }

  private void reportMetric(Future<Void> future, String metricTypeId, String postFix, String unit, String type, SocketAddress address) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    String addressId = address.host() + ":" + address.port();
    String metricId = metricBasename + "net.client." + addressId +postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    createMetricType(body, fut1);
    fut1.compose(aVoid -> {
      JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricId));
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, netClientResourceId);
      createMetric(path, body1,fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      associateMetricTypeWithResourceType(metricTypeId, netClientResourceTypeId, future);
    }, future);
  }
}

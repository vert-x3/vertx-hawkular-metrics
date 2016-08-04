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
 * Report the datagram socket Resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class DatagramSocketResourceReporter extends EntityReporter {

  private final Set<SocketAddress> sentAddresses;
  private final Set<SocketAddress> receivedAddresses;
  private static final String datagramSocketResourceTypeId = "rt.datagram";
  private static final String bytesSentMetricTypeId = "mt.counter.bytesSent";
  private static final String bytesReceivedMetricTypeId = "mt.counter.bytesReceived";
  private static final String errorCountMetricTypeId = "mt.counter.errorCount";
  private final String datagramSocketResourceId;

  DatagramSocketResourceReporter(VertxHawkularOptions options, HttpClient httpClient) {
    super(options, httpClient);
    datagramSocketResourceId = rootResourceId + ".datagram";
    sentAddresses = new HashSet<>();
    receivedAddresses = new HashSet<>();
  }
  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    createResourceType(new JsonObject().put("id", datagramSocketResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", datagramSocketResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + datagramSocketResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      List<Future> futureList = new ArrayList<>();
      Future errorCountFut = Future.future();
      reportMetric(errorCountFut, errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER", null);
      sentAddresses.forEach(addr -> {
        Future fut = Future.future();
        futureList.add(fut);
        reportMetric(fut, bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER", addr);
      });
      receivedAddresses.forEach(addr -> {
        Future fut = Future.future();
        futureList.add(fut);
        reportMetric(fut, bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER", addr);
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

  private void reportMetric(Future<Void> future, String metricTypeId, String postFix, String unit, String type, SocketAddress address) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    String metricId;
    if (address != null) {
      String addressId = address.host() + ":" + address.port();
      metricId = metricBasename + "datagram" + addressId + postFix;
    } else {
      metricId = metricBasename + "datagram" + postFix;
    }
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    createMetricType(body, fut1);
    fut1.compose(aVoid -> {
      JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricId));
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, datagramSocketResourceId);
      createMetric(path, body1,fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      associateMetricTypeWithResourceType(metricTypeId, datagramSocketResourceTypeId, future);
    }, future);
  }

  protected void addSentAddress(SocketAddress address) {
    sentAddresses.add(address);
  }

  protected void addReceivedAddress(SocketAddress address) {
    receivedAddresses.add(address);
  }
}

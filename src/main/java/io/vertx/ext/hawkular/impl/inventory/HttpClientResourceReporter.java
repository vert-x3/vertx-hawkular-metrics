package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Report the http client resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class HttpClientResourceReporter extends EntityReporter {
  private static final Logger LOG = LoggerFactory.getLogger(HttpClientResourceReporter.class);
  private final Set<SocketAddress> remoteAddresses = new HashSet<>();
  private static final String httpClientResourceTypeId = "rt.http.client";
  private final String httpClientResourceId;
  private static final String connectionsMetricTypeId = "mt.gauge.connections";
  private static final String requestsMetricTypeId = "mt.gauge.requests";
  private static final String wsConnectionsMetricTypeId = "mt.gauge.wsConnections";
  private static final String bytesReceivedMetricTypeId = "mt.counter.bytesReceived";
  private static final String bytesSentMetricTypeId = "mt.counter.bytesSent";
  private static final String errorCountMetricTypeId = "mt.counter.errorCount";
  private static final String requestCountMetricTypeId = "mt.counter.requestCount";
  private static final String responseTimeMetricTypeId = "mt.counter.responseTime";
  private static final int numMetrics = 8;

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

  private void reportAddressMetric(SocketAddress address, Future<Void> future) {

    List<Future> futureList = new ArrayList<>(numMetrics);
    for (int i = 0; i < numMetrics; i++) {
        futureList.add(Future.future());
    }
    // TCP metrics
    reportMetric(futureList.get(0), connectionsMetricTypeId, ".connections", "NONE", "GAUGE", address);
    reportMetric(futureList.get(1), bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER", address);
    reportMetric(futureList.get(2), bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER", address);
    reportMetric(futureList.get(3), errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER", address);

    // HTTP metrics
    reportMetric(futureList.get(4), requestsMetricTypeId, ".requests", "NONE", "GAUGE", address);
    reportMetric(futureList.get(5), requestCountMetricTypeId, ".requestCount", "NONE", "COUNTER", address);
    reportMetric(futureList.get(6), responseTimeMetricTypeId, ".responseTime", "MILLISECONDS", "COUNTER", address);
    reportMetric(futureList.get(7), wsConnectionsMetricTypeId, ".wsConnections", "NONE", "GAUGE", address);

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
    String metricId = metricBasename + "http.client." + addressId +postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    createMetricType(body, fut1);
    fut1.compose(aVoid -> {
      JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricId));
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, httpClientResourceId);
      createMetric(path, body1,fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      associateMetricTypeWithResourceType(metricTypeId, httpClientResourceTypeId, future);
    }, future);
  }

  protected void addRemoteAddress(SocketAddress address) {
    remoteAddresses.add(address);
  }
}

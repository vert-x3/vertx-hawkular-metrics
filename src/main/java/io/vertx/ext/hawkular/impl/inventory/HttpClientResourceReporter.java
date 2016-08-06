package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.HashSet;
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
  protected void register() {
    addEntity(feedPath, "resourceType", new JsonObject().put("id", httpClientResourceTypeId));
    JsonObject body = new JsonObject().put("id", httpClientResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + httpClientResourceTypeId);
    addEntity(rootResourcePath, "resource", body);
    remoteAddresses.forEach(addr -> {
      reportAddressMetric(addr);
    });
  }

  private void reportAddressMetric(SocketAddress address) {
    // TCP metrics
    reportMetric(connectionsMetricTypeId, ".connections", "NONE", "GAUGE", address);
    reportMetric(bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER", address);
    reportMetric(bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER", address);
    reportMetric(errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER", address);

    // HTTP metrics
    reportMetric(requestsMetricTypeId, ".requests", "NONE", "GAUGE", address);
    reportMetric(requestCountMetricTypeId, ".requestCount", "NONE", "COUNTER", address);
    reportMetric(responseTimeMetricTypeId, ".responseTime", "MILLISECONDS", "COUNTER", address);
    reportMetric(wsConnectionsMetricTypeId, ".wsConnections", "NONE", "GAUGE", address);
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type, SocketAddress address) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    String addressId = address.host() + ":" + address.port();
    String metricId = metricBasename + "http.client." + addressId +postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, "metricType", body);
    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
            .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("%s/r;%s", rootResourcePath, httpClientResourceId);
    addEntity(path, "metric", body1);
    //  associateMetricTypeWithResourceType(metricTypeId, httpClientResourceTypeId, future);
  }

  protected void addRemoteAddress(SocketAddress address) {
    remoteAddresses.add(address);
  }
}

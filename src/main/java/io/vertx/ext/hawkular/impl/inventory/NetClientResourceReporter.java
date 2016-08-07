package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.HashSet;
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

  NetClientResourceReporter(VertxHawkularOptions options) {
    super(options);
    netClientResourceId = rootResourceId + ".net.client";
  }

  protected JsonObject buildPayload() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", netClientResourceTypeId));
    JsonObject body = new JsonObject().put("id", netClientResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + netClientResourceTypeId);
    addEntity(rootResourcePath, RESOURCE, body);
    remoteAddresses.forEach(addr -> {
      reportMetric(connectionsMetricTypeId, ".connections", "NONE", "GAUGE", addr);
      reportMetric(bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER", addr);
      reportMetric(bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER", addr);
      reportMetric(errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER", addr);
    });
    return bulkJson;
  }


  protected void addRemoteAddress(SocketAddress address) {
    remoteAddresses.add(address);
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type, SocketAddress address) {
    String addressId = address.host() + ":" + address.port();
    String metricId = metricBasename + "net.client." + addressId +postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, METRIC_TYPE, body);
    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
            .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, netClientResourceId);
    addEntity(feedPath, METRIC, body1);
    addEntity(feedPath, METRIC, body1);
    addEntity(path, RELATIONSHIP, new JsonObject().put("name", "incorporates").put("otherEnd", feedPath + "/m;" + metricId).put("direction", "outgoing"));
  }
}

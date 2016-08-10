package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;


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

  NetServerResourceReporter(VertxHawkularOptions options, SocketAddress localAddress) {
    super(options);
    this.localAddress = localAddress;
    netServerResourceId = rootResourceId + ".net.server."+localAddress.host()+":"+localAddress.port();
  }

  protected JsonObject buildPayload() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", netServerResourceTypeId));
    JsonObject body = new JsonObject().put("id", netServerResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + netServerResourceTypeId);
    addEntity(rootResourcePath, RESOURCE, body);
    reportMetric(connectionsMetricTypeId, ".connections", "NONE", "GAUGE");
    reportMetric(errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER");
    reportMetric(bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER");
    reportMetric(bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER");
    return bulkJson;
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type) {
    String metricId = metricBasename+"net.server."+localAddress.host()+":"+localAddress.port()+postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, METRIC_TYPE, body);

    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", feedPath + "/mt;" + metricTypeId)
            .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("%s/r;%s", rootResourcePath, netServerResourceId);
    addEntity(feedPath, METRIC, body1);
    addEntity(path, RELATIONSHIP, new JsonObject().put("name", "incorporates").put("otherEnd", feedPath + "/m;" + metricId).put("direction", "outgoing"));
  }
}

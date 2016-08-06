package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.http.HttpClient;
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

  NetServerResourceReporter(VertxHawkularOptions options, HttpClient httpClient, SocketAddress localAddress) {
    super(options, httpClient);
    this.localAddress = localAddress;
    netServerResourceId = rootResourceId + ".net.server."+localAddress.host()+":"+localAddress.port();
  }

  @Override
  protected void register() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", netServerResourceTypeId));
    JsonObject body = new JsonObject().put("id", netServerResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + netServerResourceTypeId);
    addEntity(rootResourcePath, RESOURCE, body);
    reportMetric(connectionsMetricTypeId, ".connections", "NONE", "GAUGE");
    reportMetric(errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER");
    reportMetric(bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER");
    reportMetric(bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER");
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type) {
    String metricId = metricBasename+"net.server."+localAddress.host()+":"+localAddress.port()+postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, METRIC_TYPE, body);

    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
            .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, netServerResourceId);
    addEntity(path, METRIC, body1);
    //  associateMetricTypeWithResourceType(metricTypeId, netServerResourceTypeId, future);
  }
}

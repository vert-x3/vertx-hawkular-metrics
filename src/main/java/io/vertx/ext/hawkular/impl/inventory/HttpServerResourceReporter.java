package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report the http server resource to the Hawkular server.
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

  private final String httpServerResourceId;
  
  HttpServerResourceReporter(VertxHawkularOptions options, SocketAddress localAddress) {
    super(options);
    this.localAddress = localAddress;
    httpServerResourceId = rootResourceId + ".http.server."+localAddress.host()+":"+localAddress.port();
  }

  protected JsonObject buildPayload() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", httpServerResourceTypeId));
    JsonObject body = new JsonObject().put("id", httpServerResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + httpServerResourceTypeId);
    addEntity(rootResourcePath, RESOURCE, body);

    reportMetric(requestCountMetricTypeId, ".requestCount", "NONE", "COUNTER");
    reportMetric(processingTimeMetricTypeId, ".processingTime", "MILLISECONDS", "COUNTER");
    reportMetric(bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER");
    reportMetric(bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER");
    reportMetric(errorCountMetricTypeId, ".errorCount", "BYTES", "COUNTER");
    reportMetric(requestsMetricTypeId, ".requests", "NONE", "GAUGE");
    reportMetric(httpConnectionsMetricTypeId, ".httpConnections", "NONE", "GAUGE");
    reportMetric(wsConnectionsMetricTypeId, ".wsConnections", "NONE", "GAUGE");
    return bulkJson;
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type) {
    String metricId = metricBasename+"http.server."+localAddress.host()+":"+localAddress.port()+postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, METRIC_TYPE, body);
    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", feedPath + "/mt;" + metricTypeId)
            .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("%s/r;%s", rootResourcePath, httpServerResourceId);
    addEntity(feedPath, METRIC, body1);
    addEntity(feedPath, METRIC, body1);
    addEntity(path, RELATIONSHIP, new JsonObject().put("name", "incorporates").put("otherEnd", feedPath + "/m;" + metricId).put("direction", "outgoing"));
  }
}

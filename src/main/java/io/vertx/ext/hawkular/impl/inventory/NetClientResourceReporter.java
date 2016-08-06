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
  protected void register() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", netClientResourceTypeId));
    JsonObject body = new JsonObject().put("id", netClientResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + netClientResourceTypeId);
    addEntity(rootResourcePath, RESOURCE, body);
    remoteAddresses.forEach(addr -> {
      reportAddressMetric(addr);
    });
  }


  protected void addRemoteAddress(SocketAddress address) {
    remoteAddresses.add(address);
  }

  private void reportAddressMetric(SocketAddress address) {
    reportMetric(connectionsMetricTypeId, ".connections", "NONE", "GAUGE", address);
    reportMetric(bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER", address);
    reportMetric(bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER", address);
    reportMetric(errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER", address);
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type, SocketAddress address) {
    String addressId = address.host() + ":" + address.port();
    String metricId = metricBasename + "net.client." + addressId +postFix;
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, METRIC_TYPE, body);
    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
            .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, netClientResourceId);
    addEntity(path, METRIC, body1);
  //  associateMetricTypeWithResourceType(metricTypeId, netClientResourceTypeId, future);

  }
}

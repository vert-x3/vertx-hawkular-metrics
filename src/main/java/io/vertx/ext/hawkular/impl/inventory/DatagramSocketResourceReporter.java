package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.HashSet;
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

  DatagramSocketResourceReporter(VertxHawkularOptions options) {
    super(options);
    datagramSocketResourceId = rootResourceId + ".datagram";
    sentAddresses = new HashSet<>();
    receivedAddresses = new HashSet<>();
  }

  protected JsonObject buildPayload() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", datagramSocketResourceTypeId));
    JsonObject body = new JsonObject().put("id", datagramSocketResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + datagramSocketResourceTypeId);
    addEntity(rootResourcePath, RESOURCE, body);
    reportMetric(errorCountMetricTypeId, ".errorCount", "NONE", "COUNTER", null);
    sentAddresses.forEach(addr -> {
      reportMetric(bytesSentMetricTypeId, ".bytesSent", "BYTES", "COUNTER", addr);
    });
    receivedAddresses.forEach(addr -> {
      reportMetric(bytesReceivedMetricTypeId, ".bytesReceived", "BYTES", "COUNTER", addr);
    });
    return bulkJson;
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type, SocketAddress address) {
    String metricId;
    if (address != null) {
      String addressId = address.host() + ":" + address.port();
      metricId = metricBasename + "datagram." + addressId + postFix;
    } else {
      metricId = metricBasename + "datagram" + postFix;
    }
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, "metricType", body);
    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("/t;%s/f;%s/r;%s/r;%s", tenant, feedId, rootResourceId, datagramSocketResourceId);
    addEntity(feedPath, METRIC, body1);
    addEntity(path, RELATIONSHIP, new JsonObject().put("name", "incorporates").put("otherEnd", feedPath + "/m;" + metricId).put("direction", "outgoing"));
  }

  protected void addSentAddress(SocketAddress address) {
    sentAddresses.add(address);
  }

  protected void addReceivedAddress(SocketAddress address) {
    receivedAddresses.add(address);
  }
}

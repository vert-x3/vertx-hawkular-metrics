package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.HashSet;
import java.util.Set;

/**
 * Report the eventbus Resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class EventbusResourceReporter extends EntityReporter {


  private final String eventbusResourceTypeId = "rt.eventbus";
  private final String handlerMetricTypeId = "mt.gauge.handlers";
  private final String bytesWrittenMetricTypeId = "mt.counter.bytesWritten";
  private final String bytesReadMetricTypeId = "mt.counter.bytesRead";
  private final String pendingMetricTypeId = "mt.gauge.pending";
  private final String pendingLocalMetricTypeId = "mt.gauge.pendingLocal";
  private final String pendingRemoteMetricTypeId = "mt.gauge.pendingRemote";
  private final String publishedMessagesMetricTypeId = "mt.counter.publishedMessages";
  private final String publishedLocalMessagesMetricTypeId = "mt.counter.publishedLocalMessages";
  private final String publishedRemoteMessagesMetricTypeId = "mt.counter.publishedRemoteMessages";
  private final String sentMessagesMetricTypeId = "mt.counter.sentMessages";
  private final String sentLocalMessagesMetricTypeId = "mt.counter.sentLocalMessages";
  private final String sentRemoteMessagesMetricTypeId = "mt.counter.sentRemoteMessages";
  private final String receivedMessagesMetricTypeId = "mt.counter.receivedMessages";
  private final String receivedLocalMessagesMetricTypeId = "mt.counter.receivedLocalMessages";
  private final String receivedRemoteMessagesMetricTypeId = "mt.counter.receivedRemoteMessages";
  private final String deliveredMessagesMetricTypeId = "mt.counter.deliveredMessages";
  private final String deliveredLocalMessagesMetricTypeId = "mt.counter.deliveredLocalMessages";
  private final String deliveredRemoteMessagesMetricTypeId = "mt.counter.deliveredRemoteMessages";
  private final String replyFailuresMetricTypeId = "mt.counter.replyFailures";
  private final String processingTimeMetricTypeId = "mt.counter.processingTime";
  private final String eventbusResourceId;
  private final Set<String> remoteAddresses;

  EventbusResourceReporter(VertxHawkularOptions options) {
    super(options);
    eventbusResourceId = rootResourceId + ".eventbus";
    remoteAddresses = new HashSet<>();
  }

  protected JsonObject buildPayload() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", eventbusResourceTypeId));
    JsonObject body = new JsonObject().put("id", eventbusResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + eventbusResourceTypeId);
    addEntity(rootResourcePath, RESOURCE, body);
    reportMetric(handlerMetricTypeId, ".handlers", "NONE", "GAUGE", null);
    reportMetric(eventbusResourceTypeId, ".errorCount", "NONE", "COUNTER", null);
    reportMetric(bytesWrittenMetricTypeId, ".bytesWritten", "BYTES", "COUNTER", null);
    reportMetric(bytesReadMetricTypeId, ".bytesRead", "BYTES", "COUNTER", null);
    reportMetric(pendingMetricTypeId, ".pending", "NONE", "GAUGE", null);
    reportMetric(pendingLocalMetricTypeId, ".pendingLocal", "NONE", "GAUGE", null);
    reportMetric(pendingRemoteMetricTypeId, ".pendingRemote", "NONE", "GAUGE", null);
    reportMetric(publishedMessagesMetricTypeId, ".publishedMessages", "NONE", "COUNTER", null);
    reportMetric(publishedLocalMessagesMetricTypeId, ".publishedLocalMessages", "NONE", "COUNTER", null);
    reportMetric(publishedRemoteMessagesMetricTypeId, ".publishedRemoteMessages", "NONE", "COUNTER", null);
    reportMetric(sentMessagesMetricTypeId, ".sentMessages", "NONE", "COUNTER", null);
    reportMetric(sentLocalMessagesMetricTypeId, ".sentLocalMessages", "NONE", "COUNTER", null);
    reportMetric(sentRemoteMessagesMetricTypeId, ".sentRemoteMessages", "NONE", "COUNTER", null);
    reportMetric(receivedMessagesMetricTypeId, ".receivedMessages", "NONE", "COUNTER", null);
    reportMetric(receivedLocalMessagesMetricTypeId, ".receivedLocalMessages", "NONE", "COUNTER", null);
    reportMetric(receivedRemoteMessagesMetricTypeId, ".receivedRemoteMessages", "NONE", "COUNTER", null);
    reportMetric(deliveredMessagesMetricTypeId, ".deliveredMessages", "NONE", "COUNTER", null);
    reportMetric(deliveredLocalMessagesMetricTypeId, ".deliveredLocalMessages", "NONE", "COUNTER", null);
    reportMetric(deliveredRemoteMessagesMetricTypeId, ".deliveredRemoteMessages", "NONE", "COUNTER", null);
    reportMetric(replyFailuresMetricTypeId, ".replyFailures", "NONE", "COUNTER", null);
    remoteAddresses.forEach(addr -> {
      reportMetric(processingTimeMetricTypeId, ".processingTime", "MILLISECONDS", "COUNTER", addr);
    });
    return bulkJson;
  }

  private void reportMetric(String metricTypeId, String postFix, String unit, String type, String address) {
    String metricId;
    if (address != null) {
      metricId = metricBasename + "eventbus." + address + postFix;
    } else {
      metricId = metricBasename + "eventbus" + postFix;
    }
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    addEntity(feedPath, METRIC_TYPE, body);
    JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", feedPath + "/mt;" + metricTypeId)
            .put("properties", new JsonObject().put("metric-id", metricId));
    String path = String.format("%s/r;%s", rootResourcePath, eventbusResourceId);
    addEntity(feedPath, METRIC, body1);
    addEntity(path, RELATIONSHIP, new JsonObject().put("name", "incorporates").put("otherEnd", feedPath + "/m;" + metricId).put("direction", "outgoing"));
  }

  protected void addRemoteAddress(String address) {
    remoteAddresses.add(address);
  }
}

package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  private static final int numMetrics = 20;
  private final Set<String> remoteAddresses;

  EventbusResourceReporter(VertxHawkularOptions options, HttpClient httpClient) {
    super(options, httpClient);
    eventbusResourceId = rootResourceId + ".eventbus";
    remoteAddresses = new HashSet<>();
  }
  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();

    createResourceType(new JsonObject().put("id", eventbusResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", eventbusResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + eventbusResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      List<Future> futureList = new ArrayList<>(numMetrics);
      for (int i = 0; i < numMetrics; i++) {
        futureList.add(Future.future());
      }
      reportMetric(futureList.get(0), handlerMetricTypeId, ".handlers", "NONE", "GAUGE", null);
      reportMetric(futureList.get(1), eventbusResourceTypeId, ".errorCount", "NONE", "COUNTER", null);
      reportMetric(futureList.get(2), bytesWrittenMetricTypeId, ".bytesWritten", "BYTES", "COUNTER", null);
      reportMetric(futureList.get(3), bytesReadMetricTypeId, ".bytesRead", "BYTES", "COUNTER", null);
      reportMetric(futureList.get(4), pendingMetricTypeId, ".pending", "NONE", "GAUGE", null);
      reportMetric(futureList.get(5), pendingLocalMetricTypeId, ".pendingLocal", "NONE", "GAUGE", null);
      reportMetric(futureList.get(6), pendingRemoteMetricTypeId, ".pendingRemote", "NONE", "GAUGE", null);
      reportMetric(futureList.get(7), publishedMessagesMetricTypeId, ".publishedMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(8), publishedLocalMessagesMetricTypeId, ".publishedLocalMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(9), publishedRemoteMessagesMetricTypeId, ".publishedRemoteMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(10), sentMessagesMetricTypeId, ".sentMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(11), sentLocalMessagesMetricTypeId, ".sentLocalMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(12), sentRemoteMessagesMetricTypeId, ".sentRemoteMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(13), receivedMessagesMetricTypeId, ".receivedMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(14), receivedLocalMessagesMetricTypeId, ".receivedLocalMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(15), receivedRemoteMessagesMetricTypeId, ".receivedRemoteMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(16), deliveredMessagesMetricTypeId, ".deliveredMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(17), deliveredLocalMessagesMetricTypeId, ".deliveredLocalMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(18), deliveredRemoteMessagesMetricTypeId, ".deliveredRemoteMessages", "NONE", "COUNTER", null);
      reportMetric(futureList.get(19), replyFailuresMetricTypeId, ".replyFailures", "NONE", "COUNTER", null);
      remoteAddresses.forEach(addr -> {
        Future fut = Future.future();
        futureList.add(fut);
        reportMetric(fut, processingTimeMetricTypeId, ".processingTime", "MILLISECONDS", "COUNTER", addr);
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

  private void reportMetric(Future<Void> future, String metricTypeId, String postFix, String unit, String type, String address) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    String metricId;
    if (address != null) {
      metricId = metricBasename + "eventbus." + address + postFix;
    } else {
      metricId = metricBasename + "eventbus" + postFix;
    }
    JsonObject body = new JsonObject().put("id", metricTypeId).put("type", type).put("unit", unit).put("collectionInterval", collectionInterval);
    createMetricType(body, fut1);
    fut1.compose(aVoid -> {
      JsonObject body1 = new JsonObject().put("id", metricId).put("metricTypePath", "/f;" + feedId + "/mt;" + metricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricId));
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, eventbusResourceId);
      createMetric(path, body1,fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      associateMetricTypeWithResourceType(metricTypeId, eventbusResourceTypeId, future);
    }, future);
  }

  protected void addRemoteAddress(String address) {
    remoteAddresses.add(address);
  }
}

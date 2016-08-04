package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report the eventbus Resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class EventbusResourceReporter extends EntityReporter {

  private final String eventbusResourceTypeId = "rt.eventbus";
  private final String gaugeMetricTypeId = "mt.gauge";
  private final String eventbusHandlerMetricId = "m.eventbus.handlers";
  private final String eventbusResourceId;


  EventbusResourceReporter(VertxHawkularOptions options, HttpClient httpClient) {
    super(options, httpClient);
    eventbusResourceId = rootResourceId + ".eventbus";
  }
  @Override
  void report(Future<Void> future) {
    Future<Void> fut1 = Future.future();
    Future<Void> fut2 = Future.future();
    Future<Void> fut3 = Future.future();
    Future<Void> fut4 = Future.future();
    Future<Void> fut5 = Future.future();

    createResourceType(new JsonObject().put("id", eventbusResourceTypeId), fut1);
    fut1.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", eventbusResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;" + eventbusResourceTypeId);
      createResource("f;" + feedId + "/r;" + rootResourceId, body, fut2);
    }, fut2);
    fut2.compose(aVoid -> {
      JsonObject body = new JsonObject().put("id", gaugeMetricTypeId).put("type", "GAUGE").put("unit", "NONE").put("collectionInterval", collectionInterval);
      createMetricType( body, fut3);
    }, fut3);
    fut3.compose(aVoid -> {
      String path = String.format("f;%s/r;%s/r;%s", feedId, rootResourceId, eventbusResourceId);
      JsonObject body = new JsonObject().put("id", eventbusHandlerMetricId).put("metricTypePath", "/f;" + feedId + "/mt;" + gaugeMetricTypeId)
              .put("properties", new JsonObject().put("metric-id", metricBasename+"eventbus.handlers"));
      createMetric(path, body, fut4);
    }, fut4);
    fut4.compose(aVoid -> {
      associateMetricTypeWithResourceType(gaugeMetricTypeId, eventbusResourceTypeId, fut5);
    }, fut5);
    fut5.setHandler(ar -> {
      if (ar.succeeded()){
        future.complete();
      } else {
        future.fail(ar.cause());
      }
    });
  }
}

package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report a single entity to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class EntityReporter {

  protected static String feedId;
  protected static String tenant;
  protected static String metricBasename;
  protected static String rootResourceTypeId = "rt.vertx-root";
  protected static String rootResourceId;
  protected static int collectionInterval;
  protected static String tenantPath;
  protected static String feedPath;
  protected static String rootResourcePath;

  protected static final String FEED = "feed";
  protected static final String RESOURCE = "resource";
  protected static final String RESOURCE_TYPE = "resourceType";
  protected static final String METRIC = "metric";
  protected static final String METRIC_TYPE = "metricType";
  protected static final String RELATIONSHIP = "relationship";

  protected JsonObject bulkJson = new JsonObject();

  EntityReporter(VertxHawkularOptions options) {
    feedId = options.getFeedId();
    rootResourceId = options.getVertxRootResourceId();
    if (feedId == null || feedId.isEmpty() || rootResourceId == null || rootResourceId.isEmpty()) {
      throw new IllegalArgumentException("feed id and root resource id must not be null.");
    }
    metricBasename = options.getPrefix() + (options.getPrefix().isEmpty() ? "" : ".") + "vertx.";
    collectionInterval = options.getSchedule();
    tenant = options.getTenant();
    tenantPath = String.format("/t;%s", tenant);
    feedPath = String.format("%s/f;%s", tenantPath, feedId);
    rootResourcePath = String.format("%s/r;%s", feedPath, rootResourceId);
  }

  protected void addEntity(String path, String type, JsonObject entity) {
    if (!bulkJson.containsKey(path)) {
      bulkJson.put(path, new JsonObject());
    }
    if (!bulkJson.getJsonObject(path).containsKey(type)) {
      bulkJson.getJsonObject(path).put(type, new JsonArray());
    }
    bulkJson.getJsonObject(path).getJsonArray(type).add(entity);
  }

  protected JsonObject buildPayload() {
    return new JsonObject();
  }
}

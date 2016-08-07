package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report the root resource to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class RootResourceReporter extends EntityReporter {

  private final String type;
  RootResourceReporter(VertxHawkularOptions options, HttpClient httpClient, String type) {
    super(options);
    this.type = type;
  }

  protected JsonObject buildPayload() {
    addEntity(feedPath, RESOURCE_TYPE, new JsonObject().put("id", rootResourceTypeId));
    JsonObject body = new JsonObject()
            .put("id", rootResourceId)
            .put("resourceTypePath", feedPath + "/rt;" + rootResourceTypeId)
            .put("properties", new JsonObject().put("type", type));
    addEntity(feedPath, RESOURCE,body);
    return bulkJson;
  }
}

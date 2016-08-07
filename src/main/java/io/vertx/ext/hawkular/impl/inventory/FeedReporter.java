package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report the vertx feed to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class FeedReporter extends EntityReporter {

  FeedReporter(VertxHawkularOptions options) {
    super(options);
  }
  @Override
  protected JsonObject buildPayload() {
    addEntity(tenantPath, FEED, new JsonObject().put("id", feedId));
    return bulkJson;
  }
}

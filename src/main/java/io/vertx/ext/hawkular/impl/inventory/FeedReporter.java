package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * Report the vertx feed to the Hawkular server.
 *
 * @author Austin Kuo
 */
public class FeedReporter extends EntityReporter {

  FeedReporter(VertxHawkularOptions options, HttpClient httpClient) {
    super(options, httpClient);
  }
  @Override
  protected void register() {
    addEntity(tenantPath, FEED, new JsonObject().put("id", feedId));
  }
}

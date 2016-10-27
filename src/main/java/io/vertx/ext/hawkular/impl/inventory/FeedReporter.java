package io.vertx.ext.hawkular.impl.inventory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
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
  void report(Future<Void> future) {
    createFeed(future);
  }
}

/*
 * Copyright 2015 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.hawkular.examples;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * @author Thomas Segismont
 */
public class HttpServerExample {

  public static void main(String[] args) {
    VertxHawkularOptions vertxHawkularOptions = new VertxHawkularOptions()
      .setHttpOptions(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(8080))
      .setTenant("default")
      .setPrefix("instance1")
      .setSchedule(3);
    vertxHawkularOptions.setEnabled(true);

    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setMetricsOptions(vertxHawkularOptions);

    Vertx vertx = Vertx.vertx(vertxOptions);

    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(req -> {
      req.response().setChunked(true).putHeader("Content-Type", "text/plain").write("some text").end();
    });
    httpServer.listen(9191);
  }
}

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
import io.vertx.core.net.NetServer;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * @author Thomas Segismont
 */
public class NetServerExample {

  public static void main(String[] args) {
    VertxHawkularOptions vertxHawkularOptions = new VertxHawkularOptions();
    vertxHawkularOptions.setPrefix("instance1");
    vertxHawkularOptions.setEnabled(true);

    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setMetricsOptions(vertxHawkularOptions);

    Vertx vertx = Vertx.vertx(vertxOptions);

    NetServer server = vertx.createNetServer();
    // Connect with telnet, write some text + Enter
    server.connectHandler(socket -> {
      socket.handler(buffer -> {
        socket.write("Received your request!\n").close();
      });
    });
    server.listen(9292);
  }
}

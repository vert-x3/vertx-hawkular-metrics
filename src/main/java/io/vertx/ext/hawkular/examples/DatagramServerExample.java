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
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.ext.hawkular.VertxHawkularOptions;

import java.util.Date;

/**
 * @author Thomas Segismont
 */
public class DatagramServerExample {

  public static void main(String[] args) {
    VertxHawkularOptions vertxHawkularOptions = new VertxHawkularOptions();
    vertxHawkularOptions.setPrefix("instance1");
    vertxHawkularOptions.setBatchSize(4).setBatchDelay(5);
    vertxHawkularOptions.setEnabled(true);

    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setMetricsOptions(vertxHawkularOptions);

    Vertx vertx = Vertx.vertx(vertxOptions);

    DatagramSocket datagramSocket = vertx.createDatagramSocket();
    datagramSocket.handler(p -> {
      System.out.println(p.data().toString());
    });
    datagramSocket.listen(9393, "127.0.0.1", s -> {
    });

    vertx.setPeriodic(1000, id -> {
      datagramSocket.send(String.valueOf(new Date()), 9393, "127.0.0.1", d -> {
      });
    });
  }
}

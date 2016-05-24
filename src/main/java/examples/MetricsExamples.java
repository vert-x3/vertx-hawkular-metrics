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
package examples;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.docgen.Source;
import io.vertx.ext.hawkular.HawkularServerOptions;
import io.vertx.ext.hawkular.ServerType;
import io.vertx.ext.hawkular.StandaloneMetricsOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;

/**
 * @author Thomas Segismont
 */
@Source
@SuppressWarnings("unused")
public class MetricsExamples {

  Vertx vertx;

  public void setup() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions().setEnabled(true)
    ));
  }

  public void setupRemote() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions()
        .setEnabled(true)
        .setHost("hawkular.example.com")
        .setPort(8080)
    ));
  }

  public void setupServerType() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions()
        .setEnabled(true)
        .setServerType(ServerType.HAWKULAR)
    ));
  }

  public void setupTenant() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions()
        .setEnabled(true)
        .setStandaloneMetricsOptions(new StandaloneMetricsOptions().setTenant("sales-department"))
    ));
  }

  public void setupHawkularAuth() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions()
        .setEnabled(true)
        .setServerType(ServerType.HAWKULAR)
        .setHawkularServerOptions(
          new HawkularServerOptions()
            .setId("username")
            .setSecret("password")
            .setTenant("sales-department")
        )
    ));
  }

  public void setupSecured() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions()
        .setEnabled(true)
        .setHost("hawkular.example.com")
        .setPort(443)
        .setHttpOptions(new HttpClientOptions().setSsl(true))
    ));
  }

  public void enableMetricsBridge() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions()
        .setEnabled(true)
        .setMetricsBridgeEnabled(true)
    ));
  }

  public void customMetricsBridgeAddress() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxHawkularOptions()
        .setEnabled(true)
        .setMetricsBridgeEnabled(true)
        .setMetricsBridgeAddress("__hawkular_metrics")
    ));
  }

  public void userDefinedMetric() {
    JsonObject message = new JsonObject()
      .put("id", "myapp.files.opened")
      .put("value", 7);
    vertx.eventBus().publish("metrics", message);
  }

  public void userDefinedMetricExplicit() {
    JsonObject message = new JsonObject()
      .put("id", "myapp.files.opened")
      .put("type", "counter")
      .put("timestamp", 189898098098908L)
      .put("value", 7);
    vertx.eventBus().publish("metrics", message);
  }

}

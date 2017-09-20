/*
 * Copyright 2017 Red Hat, Inc.
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

package io.vertx.ext.metric.reporters.hawkular.impl

import io.vertx.core.AbstractVerticle
import io.vertx.ext.unit.TestContext
import org.junit.Test

import java.util.concurrent.CompletableFuture

/**
 * @author Thomas Segismont
 */
class VerticleITest extends BaseITest {

  @Test
  void testVerticleMetricsValues(TestContext context) {
    def metricName = "${METRIC_PREFIX}.vertx.verticle.${SampleVerticle.class.name}"

    CompletableFuture<String> id1Future = new CompletableFuture<>();
    vertx.deployVerticle(SampleVerticle.class.name, [instances: 3], { ar ->
      if (ar.succeeded()) id1Future.complete(ar.result()) else id1Future.completeExceptionally(ar.cause())
    })

    def id1 = id1Future.get()
    assertGaugeEquals(3, tenantId, metricName)

    CompletableFuture<String> id2Future = new CompletableFuture<>();
    vertx.deployVerticle(SampleVerticle.class.name, [instances: 4], { ar ->
      if (ar.succeeded()) id2Future.complete(ar.result()) else id2Future.completeExceptionally(ar.cause())
    })

    id2Future.get()
    assertGaugeEquals(7, tenantId, metricName)

    CompletableFuture<Void> undeployFuture = new CompletableFuture<>();
    vertx.undeploy(id1, { ar ->
      if (ar.succeeded()) undeployFuture.complete(null) else undeployFuture.completeExceptionally(ar.cause())
    })

    undeployFuture.get()
    assertGaugeEquals(4, tenantId, metricName)
  }
}

class SampleVerticle extends AbstractVerticle {
  SampleVerticle() {
  }
}

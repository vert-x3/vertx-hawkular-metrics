/*
 * Copyright 2016 Red Hat, Inc.
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

package io.vertx.ext.hawkular.impl

import io.vertx.groovy.ext.unit.TestContext
import org.junit.Test

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
class EventBusBridgeITest extends BaseITest {

  def testHost = 'localhost'
  def testPort = getPort(9192)
  def verticleName = 'verticles/metrics_sender.groovy'


  @Test
  void shouldGetCustomMetrics(TestContext context) {
    def instances = 1
    def config = [
            'host': testHost,
            'port': testPort,
            'id': 'my-metric'
    ]
    deployVerticle(verticleName, config, instances, context)
    assertGaugeEquals(1.0, tenantId, "my-metric")
  }

  @Test
  void shouldGetCustomMetricsWithTimestamp(TestContext context) {
    def instances = 1
    def config = [
            'host': testHost,
            'port': testPort,
            'insert-timestamp': true,
            'id': 'my-metric-ts'
    ]
    deployVerticle(verticleName, config, instances, context)
    assertGaugeEquals(1.0, tenantId, "my-metric-ts")
  }

  @Test
  void shouldGetCustomMetricsSentAsCounter(TestContext context) {
    def instances = 1
    def config = [
            'host': testHost,
            'port': testPort,
            'counter' : true,
            'id': 'my-metric-counter'
    ]
    deployVerticle(verticleName, config, instances, context)
    assertCounterEquals(1L, tenantId, "my-metric-counter")
  }
}

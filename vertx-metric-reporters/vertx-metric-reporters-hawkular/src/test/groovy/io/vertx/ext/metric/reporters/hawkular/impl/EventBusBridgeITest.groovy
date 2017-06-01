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

package io.vertx.ext.metric.reporters.hawkular.impl

import org.junit.Test

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
class EventBusBridgeITest extends BaseITest {

  @Test
  void shouldGetCustomMetrics() {
    vertx.eventBus().send('hawkular.metrics', ['id': 'my-metric', 'timestamp': System.currentTimeMillis(), 'value': 1.0D])
    assertGaugeEquals(1.0, tenantId, "my-metric")
  }

  @Test
  void shouldGetCustomMetricsWithTimestamp() {
    vertx.eventBus().send('hawkular.metrics', ['id': 'my-metric-ts', 'value': 1.0D])
    assertGaugeEquals(1.0, tenantId, "my-metric-ts")
  }

  @Test
  void shouldGetCustomMetricsSentAsCounter() {
    vertx.eventBus().send('hawkular.metrics', ['id': 'my-metric-counter', 'type': 'counter', 'value': 1L])
    assertCounterEquals(1L, tenantId, "my-metric-counter")
  }

  @Test
  void shouldGetCustomMetricsSentAsAvailability() {
    vertx.eventBus().send('hawkular.metrics', ['id': 'my-metric-av', 'type': 'availability', 'value': 'down'])
    assertAvailabilityEquals('down', tenantId, "my-metric-av")
  }
}

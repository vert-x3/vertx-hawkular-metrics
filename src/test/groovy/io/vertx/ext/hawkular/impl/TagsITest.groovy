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

package io.vertx.ext.hawkular.impl

import org.junit.Test

/**
 * @author Thomas Segismont
 */
class TagsITest extends BaseITest {

  @Override
  protected Map createVertxOptions(String tenantId) {
    def options = super.createVertxOptions(tenantId)
    options.metricsOptions.metricTagsMatches = [
      [
        tags : [myapp: 'my-metric'],
        match: [
          type : 'REGEX',
          value: '.+\\.my-metric-(gauge|counter|av)'
        ]
      ],
      [
        tags : [myapp: 'my-metric-av'],
        match: [
          value: METRIC_PREFIX + '.' + 'my-metric-av'
        ]
      ]
    ] as Serializable
    options
  }

  @Test
  void shouldTagMetrics() {
    vertx.eventBus().send('hawkular.metrics', ['id': 'my-metric-gauge', 'value': 1.0D])
    vertx.eventBus().send('hawkular.metrics', ['id': 'my-metric-counter', 'type': 'counter', 'value': 1L])
    vertx.eventBus().send('hawkular.metrics', ['id': 'my-metric-av', 'type': 'availability', 'value': 'down'])

    assertTagsEquals([dc: 'mars01', host: 'host13', myapp: 'my-metric'], tenantId, 'gauges', "${METRIC_PREFIX}.my-metric-gauge")
    assertTagsEquals([dc: 'mars01', host: 'host13', myapp: 'my-metric'], tenantId, 'counters', "${METRIC_PREFIX}.my-metric-counter")
    assertTagsEquals([dc: 'mars01', host: 'host13', myapp: 'my-metric-av'], tenantId, 'availability', "${METRIC_PREFIX}.my-metric-av")
  }
}

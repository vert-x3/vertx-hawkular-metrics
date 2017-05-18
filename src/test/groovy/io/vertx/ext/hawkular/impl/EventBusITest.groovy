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

package io.vertx.ext.hawkular.impl

import io.vertx.ext.unit.TestContext
import org.junit.Before
import org.junit.Test

/**
 * @author Thomas Segismont
 */
class EventBusITest extends BaseITest {
  static final EVENT_BUS_METRICS = ['handlers', 'processingTime', 'errorCount', 'bytesWritten', 'bytesRead', 'pending',
                             'pendingLocal', 'pendingRemote', 'publishedMessages', 'publishedLocalMessages',
                             'publishedRemoteMessages', 'sentMessages', 'sentLocalMessages', 'sentRemoteMessages',
                             'receivedMessages', 'receivedLocalMessages', 'receivedRemoteMessages', 'deliveredMessages',
                             'deliveredLocalMessages', 'deliveredRemoteMessages', 'replyFailures']

  def String address = "testSubject"
  def baseName = "${METRIC_PREFIX}.vertx.eventbus."
  def baseNameWithAddress = "${baseName}${address}."
  def baseNameWithMetricsAddress = "${baseName}hawkular.metrics."
  def eventBus = vertx.eventBus()
  def instances = 3

  @Before
  void setup(TestContext context) {
    def verticleName = 'verticles/event_bus_handler.groovy'
    def config = [:]
    deployVerticle(verticleName, config, instances, context)
  }

  @Test
  void shouldReportEventBusMetrics() {
    def handlerSleep = 13
    def publishedNofail = 6
    publishedNofail.times { i -> eventBus.publish(address, [fail: false, sleep: handlerSleep]) }
    def publishedFail = 4
    publishedFail.times { i -> eventBus.publish(address, [fail: true, sleep: handlerSleep]) }

    def nameFilter = { String id -> id.startsWith(baseName) }
    def nameTransformer = { String id ->
      if (id.startsWith(baseNameWithMetricsAddress)) {
        return id.substring(baseNameWithMetricsAddress.length())
      }
      if (id.startsWith(baseNameWithAddress)) {
        return id.substring(baseNameWithAddress.length())
      }
      return id.substring(baseName.length())
    }
    assertMetricsEquals(EVENT_BUS_METRICS as Set, tenantId, nameFilter, nameTransformer)

    def allPublished = publishedNofail + publishedFail
    assertGaugeEquals(instances + 1 /* take metrics bridge handler into account*/, tenantId, "${baseName}handlers")
    assertCounterGreaterThan(handlerSleep * allPublished, tenantId, "${baseNameWithAddress}processingTime")
    assertCounterEquals(instances * publishedFail, tenantId, "${baseName}errorCount")
    assertCounterEquals(allPublished, tenantId, "${baseName}publishedMessages")
    assertCounterEquals(allPublished, tenantId, "${baseName}publishedLocalMessages")
    assertCounterEquals(0, tenantId, "${baseName}publishedRemoteMessages")
    assertCounterEquals(0, tenantId, "${baseName}sentMessages")
    assertCounterEquals(0, tenantId, "${baseName}sentLocalMessages")
    assertCounterEquals(0, tenantId, "${baseName}sentRemoteMessages")
    assertCounterEquals(allPublished, tenantId, "${baseName}receivedMessages")
    assertCounterEquals(allPublished, tenantId, "${baseName}receivedLocalMessages")
    assertCounterEquals(0, tenantId, "${baseName}receivedRemoteMessages")
    assertCounterEquals(allPublished, tenantId, "${baseName}deliveredMessages")
    assertCounterEquals(allPublished, tenantId, "${baseName}deliveredLocalMessages")
    assertCounterEquals(0, tenantId, "${baseName}deliveredRemoteMessages")
  }
}

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

package io.vertx.ext.metric.reporters.hawkular.impl

import io.vertx.core.datagram.DatagramSocket
import io.vertx.ext.unit.TestContext
import org.junit.Test

/**
 * @author Thomas Segismont
 */
class DatagramITest extends BaseITest {
  static final CONTENT = 'some text'
  static final DATAGRAM_METRICS = ['bytesReceived', 'bytesSent', 'errorCount']

  def testHost = 'localhost'
  def testPort = getPort(9192)
  def baseName = "${METRIC_PREFIX}.vertx.datagram."
  def baseNameWithAddress = "${baseName}${testHost}:${testPort}."

  def DatagramSocket client

  @Override
  void setUp(TestContext context) throws Exception {
    super.setUp(context)
    def verticleName = 'verticles/datagram_server.groovy'
    def instances = 1
    def config = [
      'host': testHost,
      'port': testPort
    ]
    deployVerticle(verticleName, config, instances, context)
    client = vertx.createDatagramSocket()
  }

  @Test
  void shouldReportDatagramMetrics(TestContext context) {
    def sentCount = 5
    sentCount.times { i -> client.send(CONTENT, testPort, testHost, context.asyncAssertSuccess()) }

    def nameFilter = { String id -> id.startsWith(baseName) }
    def nameTransformer = { String id ->
      id.startsWith(baseNameWithAddress) ? id.substring(baseNameWithAddress.length()) : id.substring(baseName.length())
    }
    assertMetricsEquals(DATAGRAM_METRICS as Set, tenantId, nameFilter, nameTransformer)

    assertCounterEquals(sentCount * CONTENT.bytes.length, tenantId, "${baseNameWithAddress}bytesReceived")
    assertCounterEquals(sentCount * CONTENT.bytes.length, tenantId, "${baseNameWithAddress}bytesSent")
    assertCounterEquals(0, tenantId, "${baseName}errorCount")
  }
}

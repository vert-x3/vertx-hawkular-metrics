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

import io.vertx.groovy.ext.unit.TestContext
import org.junit.Before
import org.junit.Test

/**
 * @author Thomas Segismont
 */
class NetServerITest extends BaseITest {
  static final RESPONSE_CONTENT = 'some text'
  static final NET_SERVER_METRICS = ['bytesReceived', 'bytesSent', 'errorCount', 'connections']

  def testHost = '127.0.0.1'
  def testPort = getPort(9193)
  def metricPrefix = "${METRIC_PREFIX}.vertx.net.server.${testHost}:${testPort}."

  @Before
  void setup(TestContext context) {
    def verticleName = 'verticles/net_server.groovy'
    def instances = 4
    def config = [
      'host'   : testHost,
      'port'   : testPort,
      'content': RESPONSE_CONTENT
    ]
    deployVerticle(verticleName, config, instances, context)
  }

  @Test
  void shouldReportNetServerMetrics() {
    def nameFilter = { String id -> id.startsWith(metricPrefix) }
    def nameTransformer = { String id -> id.substring(metricPrefix.length()) }
    assertMetricsEquals(NET_SERVER_METRICS as Set, tenantId, nameFilter, nameTransformer)
  }

  @Test
  void testNetServerMetricsValues(TestContext context) {
    def requestContent = 'pitchoune'
    def sentCount = 68
    def netClient = vertx.createNetClient()
    (1..sentCount).collect { i ->
      def async = context.async()
      netClient.connect(testPort, testHost, { res ->
        if (res.failed()) {
          async.complete()
          context.fail(res.cause())
          return
        }
        def socket = res.result().exceptionHandler({ t ->
          async.complete()
          context.fail(t)
        })
        socket.write(requestContent)
        socket.closeHandler({ aVoid ->
          async.complete()
        })
      })
      async
    }.each { async ->
      async.await()
    }
    netClient.close()

    assertCounterEquals(sentCount * requestContent.bytes.length, tenantId, "${metricPrefix}bytesReceived")
    assertCounterEquals(sentCount * RESPONSE_CONTENT.bytes.length, tenantId, "${metricPrefix}bytesSent")
  }
}

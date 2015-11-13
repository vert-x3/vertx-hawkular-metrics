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
class HttpServerITest extends BaseITest {
  static final RESPONSE_CONTENT = 'some text'
  static final HTTP_SERVER_METRICS = ['bytesReceived', 'bytesSent', 'errorCount', 'httpConnections', 'processingTime',
                                      'requestCount', 'requests', 'wsConnections']

  def testHost = '127.0.0.1'
  def testPort = getPort(9191)
  def metricPrefix = "${METRIC_PREFIX}.vertx.http.server.${testHost}:${testPort}."
  def requestDelay = 11L

  @Before
  void setup(TestContext context) {
    def verticleName = 'verticles/http_server.groovy'
    def instances = 4
    def config = [
      'host'        : testHost,
      'port'        : testPort,
      'requestDelay': requestDelay,
      'content'     : RESPONSE_CONTENT
    ]
    deployVerticle(verticleName, config, instances, context)
  }

  @Test
  void shouldReportHttpServerMetrics() {
    def nameFilter = { String id -> id.startsWith(metricPrefix) }
    def nameTransformer = { String id -> id.substring(metricPrefix.length()) }
    assertMetricsEquals(HTTP_SERVER_METRICS as Set, tenantId, nameFilter, nameTransformer)
  }

  @Test
  void testHttpServerMetricsValues(TestContext context) {
    def bodyContent = 'pitchoune'
    def sentCount = 68
    def httpClient = vertx.createHttpClient([defaultHost: testHost, defaultPort: testPort])
    (1..sentCount).collect { i ->
      def async = context.async()
      httpClient.post("", { response ->
        async.complete()
        if (response.statusCode() != 200) {
          context.fail(response.statusMessage())
        }
      }).exceptionHandler({ t ->
        async.complete()
        context.fail(t)
      }).putHeader('Content-Length', bodyContent.bytes.length as String).write(bodyContent).end()
      async
    }.each { async ->
      async.await()
    }
    httpClient.close()

    assertCounterEquals(sentCount * bodyContent.bytes.length, tenantId, "${metricPrefix}bytesReceived")
    assertCounterEquals(sentCount * RESPONSE_CONTENT.bytes.length, tenantId, "${metricPrefix}bytesSent")
    assertCounterGreaterThan(sentCount * requestDelay, tenantId, "${metricPrefix}processingTime")
    assertCounterEquals(sentCount, tenantId, "${metricPrefix}requestCount")
  }
}

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

import io.vertx.core.http.HttpClient
import io.vertx.ext.unit.TestContext
import org.junit.Test

import java.util.concurrent.ForkJoinPool

/**
 * @author Thomas Segismont
 */
class HttpClientITest extends BaseITest {
  static final RESPONSE_CONTENT = 'some text'

  def testHost = 'localhost'
  def testPort = getPort(9195)
  def metricPrefix = "${METRIC_PREFIX}.vertx.http.client.${testHost}:${testPort}."
  def requestDelay = 11L

  def concurrentClients = ForkJoinPool.commonPool().parallelism
  def List<HttpClient> httpClients = []

  @Override
  void setUp(TestContext context) throws Exception {
    super.setUp(context)
    def verticleName = 'verticles/http_server.groovy'
    def instances = 1
    def config = [
      'host'        : testHost,
      'port'        : testPort,
      'content'     : RESPONSE_CONTENT,
      'requestDelay': requestDelay
    ]
    deployVerticle(verticleName, config, instances, context)
    concurrentClients.times { i ->
      def httpClient = vertx.createHttpClient([
        defaultHost: testHost,
        defaultPort: testPort
      ])
      httpClients.push(httpClient)
    }
  }

  @Test
  void testHttpClientMetricsValues(TestContext context) {
    def requestContent = 'pitchoune'
    def sentCount = 68

    concurrentClients.times { i ->
      ForkJoinPool.commonPool().execute({
        httpRequest(httpClients[i], sentCount, requestContent, context)
        wsRequest(httpClients[i], requestContent, context)
      })
    }

    assertGaugeEquals(0, tenantId, "${metricPrefix}wsConnections")
    assertCounterEquals(concurrentClients * (sentCount + 1) * requestContent.bytes.length, tenantId, "${metricPrefix}bytesReceived")
    assertCounterEquals(concurrentClients * (sentCount + 1) * RESPONSE_CONTENT.bytes.length, tenantId, "${metricPrefix}bytesSent")
    assertCounterEquals(concurrentClients * sentCount, tenantId, "${metricPrefix}requestCount")
    assertCounterGreaterThan(concurrentClients * (sentCount + 1) * requestDelay, tenantId, "${metricPrefix}responseTime")
  }

  private void httpRequest(HttpClient httpClient, int sentCount, String requestContent, TestContext context) {
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
      }).putHeader('Content-Length', requestContent.bytes.length as String).write(requestContent).end()
      async
    }.each { async ->
      async.await()
    }
  }

  private void wsRequest(HttpClient httpClient, String requestContent, TestContext context) {
    def async = context.async()
    httpClient.websocket(testPort, testHost, "", { ws ->
      ws.handler({ event ->
        async.complete()
        ws.close()
      })
      ws.writeTextMessage(requestContent)
    })
    async.await()
  }

  @Override
  void tearDown(TestContext context) throws Exception {
    concurrentClients.times { i ->
      httpClients[i].close()
    }
    super.tearDown(context)
  }
}

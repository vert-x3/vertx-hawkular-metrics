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

import io.vertx.core.net.NetClient
import io.vertx.ext.unit.TestContext
import org.junit.Test

import java.util.concurrent.ForkJoinPool

/**
 * @author Thomas Segismont
 */
class NetClientITest extends BaseITest {
  static final RESPONSE_CONTENT = 'some text'

  def testHost = 'localhost'
  def testPort = getPort(9194)
  def metricPrefix = "${METRIC_PREFIX}.vertx.net.client.${testHost}:${testPort}."

  def concurrentClients = ForkJoinPool.commonPool().parallelism
  def List<NetClient> netClients = []

  @Override
  void setUp(TestContext context) throws Exception {
    super.setUp(context)
    def verticleName = 'verticles/net_server.groovy'
    def instances = 1
    def config = [
      'host'   : testHost,
      'port'   : testPort,
      'content': RESPONSE_CONTENT
    ]
    deployVerticle(verticleName, config, instances, context)
    concurrentClients.times { i ->
      def netClient = vertx.createNetClient()
      netClients.push(netClient)
    }
  }

  @Test
  void testNetClientMetricsValues(TestContext context) {
    def requestContent = 'pitchoune'
    def sentCount = 68

    concurrentClients.times { i ->
      ForkJoinPool.commonPool().execute({
        runClient(netClients[i], sentCount, requestContent, context)
      })
    }

    assertCounterEquals(concurrentClients * sentCount * requestContent.bytes.length, tenantId, "${metricPrefix}bytesReceived")
    assertCounterEquals(concurrentClients * sentCount * RESPONSE_CONTENT.bytes.length, tenantId, "${metricPrefix}bytesSent")
  }

  private void runClient(NetClient netClient, int sentCount, String requestContent, TestContext context) {
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
        socket.handler({ buf -> })
        socket.write(requestContent)
        socket.closeHandler({ aVoid ->
          async.complete()
        })
      })
      async
    }.each { async ->
      async.await()
    }
  }

  @Override
  void tearDown(TestContext context) throws Exception {
    concurrentClients.times { i ->
      netClients[i].close()
    }
    super.tearDown(context)
  }
}

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

import io.vertx.core.WorkerExecutor
import io.vertx.ext.unit.TestContext
import org.junit.Test

/**
 * @author Thomas Segismont
 */
class NamedPoolITest extends BaseITest {
  static final NAMED_POOL_METRICS =
    ['delay', 'queued', 'queuedCount', 'usage', 'inUse', 'completed', 'maxPoolSize', 'poolRatio']

  def workerExecutorName = "test-worker"
  int maxPoolSize = 8
  def baseName = "${METRIC_PREFIX}.vertx.pool.worker."
  def baseNameWithPoolName = "${baseName}${workerExecutorName}."

  private WorkerExecutor workerExecutor

  @Override
  void setUp(TestContext context) throws Exception {
    super.setUp(context)
    workerExecutor = vertx.createSharedWorkerExecutor(workerExecutorName, maxPoolSize)
  }

  @Test
  void shouldReportNamedPoolMetrics() {
    def nameFilter = { String id -> id.startsWith(baseNameWithPoolName) }
    def nameTransformer = { String id ->
      id.startsWith(baseNameWithPoolName) ? id.substring(baseNameWithPoolName.length()) : id.substring(baseName.length())
    }
    assertMetricsEquals(NAMED_POOL_METRICS as Set, tenantId, nameFilter, nameTransformer)
  }

  @Test
  void testNamedPoolMetricsValues(TestContext context) {
    def taskCount = maxPoolSize * 3
    def sleepMillis = 50
    def async = context.async(taskCount)
    taskCount.times { i ->
      workerExecutor.executeBlocking({ future ->
        sleep(sleepMillis)
        async.countDown()
        future.complete()
      }, false, context.asyncAssertSuccess())
    }

    async.awaitSuccess()

    // If all tasks could be submitted *exactly* at the same time, cumulated delay would be maxPoolSize * 3 * sleepMillis
    // In practice, the value will be close, but not equal
    // So let's make sure it's at least maxPoolSize * 2 * sleepMillis
    assertCounterGreaterThan(maxPoolSize * 2 * sleepMillis, tenantId, "${baseNameWithPoolName}delay")
    assertGaugeEquals(0, tenantId, "${baseNameWithPoolName}queued")
    assertCounterEquals(taskCount, tenantId, "${baseNameWithPoolName}queuedCount")
    assertCounterGreaterThan(taskCount * sleepMillis, tenantId, "${baseNameWithPoolName}usage")
    assertGaugeEquals(0, tenantId, "${baseNameWithPoolName}inUse")
    assertCounterEquals(taskCount, tenantId, "${baseNameWithPoolName}completed")
    assertGaugeEquals(maxPoolSize, tenantId, "${baseNameWithPoolName}maxPoolSize")
    assertGaugeEquals(0, tenantId, "${baseNameWithPoolName}poolRatio")
  }
}

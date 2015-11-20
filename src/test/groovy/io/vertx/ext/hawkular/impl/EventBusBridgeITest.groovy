package io.vertx.ext.hawkular.impl

import io.vertx.groovy.ext.unit.TestContext
import org.junit.Before
import org.junit.Test

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
class EventBusBridgeITest extends BaseITest {

  def testHost = '127.0.0.1'
  def testPort = getPort(9192)
  def verticleName = 'verticles/metrics_sender.groovy'


  @Test
  void shouldGetCustomMetrics(TestContext context) {
    def instances = 1
    def config = [
            'host': testHost,
            'port': testPort,
            'source': 'my-metric'
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
            'timestamp': true,
            'source': 'my-metric-ts'
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
            'source': 'my-metric-counter'
    ]
    deployVerticle(verticleName, config, instances, context)
    assertCounterEquals(1L, tenantId, "my-metric-counter")
  }
}

package io.vertx.kotlin.ext.metrics.collector

import io.vertx.ext.metrics.collector.ExtendedMetricsOptions
import io.vertx.ext.metrics.collector.MetricsType

/**
 * A function providing a DSL for building [io.vertx.ext.metrics.collector.ExtendedMetricsOptions] objects.
 *
 * Vert.x Hawkular monitoring configuration.
 *
 * @param batchDelay  Set the maximum delay between two consecutive batches (in seconds). To reduce the number of HTTP exchanges, metric data is sent to the Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches the configured <code>batchSize</code>, or after the <code>batchDelay</code> expires. Defaults to <code>1</code> second.
 * @param batchSize  Set the maximum number of metrics in a batch. To reduce the number of HTTP exchanges, metric data is sent to the Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches the configured <code>batchSize</code>, or after the <code>batchDelay</code> expires. Defaults to <code>50</code>.
 * @param disabledMetricsTypes  Sets metrics types that are disabled.
 * @param enabled  Set whether metrics will be enabled on the Vert.x instance. Metrics are not enabled by default.
 * @param metricsBridgeAddress  Sets the metric bridge address on which the application is sending the custom metrics. Application can send metrics to this event bus address. The message is a JSON object specifying at least the <code>id</code> and <code>value</code> fields. <p/> Don't forget to also enable the bridge with <code>metricsBridgeEnabled</code>.
 * @param metricsBridgeEnabled  Sets whether or not the metrics bridge should be enabled. The metrics bridge is disabled by default.
 * @param prefix  Set the metric name prefix. Metric names are not prefixed by default. Prefixing metric names is required to distinguish data sent by different Vert.x instances.
 * @param schedule  Set the metric collection interval (in seconds). Defaults to <code>1</code>.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.ext.metrics.collector.ExtendedMetricsOptions original] using Vert.x codegen.
 */
fun ExtendedMetricsOptions(
  batchDelay: Int? = null,
  batchSize: Int? = null,
  disabledMetricsTypes: Iterable<MetricsType>? = null,
  enabled: Boolean? = null,
  metricsBridgeAddress: String? = null,
  metricsBridgeEnabled: Boolean? = null,
  prefix: String? = null,
  schedule: Int? = null): ExtendedMetricsOptions = io.vertx.ext.metrics.collector.ExtendedMetricsOptions().apply {

  if (batchDelay != null) {
    this.setBatchDelay(batchDelay)
  }
  if (batchSize != null) {
    this.setBatchSize(batchSize)
  }
  if (disabledMetricsTypes != null) {
    this.setDisabledMetricsTypes(disabledMetricsTypes.toSet())
  }
  if (enabled != null) {
    this.setEnabled(enabled)
  }
  if (metricsBridgeAddress != null) {
    this.setMetricsBridgeAddress(metricsBridgeAddress)
  }
  if (metricsBridgeEnabled != null) {
    this.setMetricsBridgeEnabled(metricsBridgeEnabled)
  }
  if (prefix != null) {
    this.setPrefix(prefix)
  }
  if (schedule != null) {
    this.setSchedule(schedule)
  }
}


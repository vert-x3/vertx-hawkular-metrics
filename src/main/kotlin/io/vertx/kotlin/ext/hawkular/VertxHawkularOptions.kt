package io.vertx.kotlin.ext.hawkular

import io.vertx.ext.hawkular.VertxHawkularOptions
import io.vertx.core.http.HttpClientOptions
import io.vertx.ext.hawkular.AuthenticationOptions
import io.vertx.ext.hawkular.MetricTagsMatch
import io.vertx.ext.hawkular.MetricsType

/**
 * A function providing a DSL for building [io.vertx.ext.hawkular.VertxHawkularOptions] objects.
 *
 * Vert.x Hawkular monitoring configuration.
 *
 * @param authenticationOptions  Set the options for authentication.
 * @param batchDelay  Set the maximum delay between two consecutive batches (in seconds). To reduce the number of HTTP exchanges, metric data is sent to the Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches the configured <code>batchSize</code>, or after the <code>batchDelay</code> expires. Defaults to <code>1</code> second.
 * @param batchSize  Set the maximum number of metrics in a batch. To reduce the number of HTTP exchanges, metric data is sent to the Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches the configured <code>batchSize</code>, or after the <code>batchDelay</code> expires. Defaults to <code>50</code>.
 * @param disabledMetricsTypes  Sets metrics types that are disabled.
 * @param enabled  Set whether metrics will be enabled on the Vert.x instance. Metrics are not enabled by default.
 * @param host  Set the Hawkular Metrics service host. Defaults to <code>localhost</code>.
 * @param httpHeaders  Set specific headers to include in HTTP requests.
 * @param httpOptions  Set the configuration of the Hawkular Metrics HTTP client.
 * @param metricTagsMatches  Sets a list of [io.vertx.ext.hawkular.MetricTagsMatch].
 * @param metricTagsMatchs  Adds a [io.vertx.ext.hawkular.MetricTagsMatch].
 * @param metricsBridgeAddress  Sets the metric bridge address on which the application is sending the custom metrics. Application can send metrics to this event bus address. The message is a JSON object specifying at least the <code>id</code> and <code>value</code> fields. <p/> Don't forget to also enable the bridge with <code>metricsBridgeEnabled</code>.
 * @param metricsBridgeEnabled  Sets whether or not the metrics bridge should be enabled. The metrics bridge is disabled by default.
 * @param metricsServiceUri  Set the Hawkular Metrics service URI. Defaults to <code>/hawkular/metrics</code>. This can be useful if you host the Hawkular server behind a proxy and manipulate the default service URI.
 * @param port  Set the Hawkular Metrics service port.  Defaults to <code>8080</code>.
 * @param prefix  Set the metric name prefix. Metric names are not prefixed by default. Prefixing metric names is required to distinguish data sent by different Vert.x instances.
 * @param schedule  Set the metric collection interval (in seconds). Defaults to <code>1</code>.
 * @param sendTenantHeader  Set whether Hawkular tenant header should be sent. Defaults to <code>true</code>. Must be set to <code>false</code> when working with pre-Alpha13 Hawkular servers.
 * @param taggedMetricsCacheSize  Set the number of metric names to cache in order to avoid repeated tagging requests.
 * @param tags  Set tags applied to all metrics.
 * @param tenant  Set the Hawkular tenant. Defaults to <code>default</code>.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.ext.hawkular.VertxHawkularOptions original] using Vert.x codegen.
 */
fun VertxHawkularOptions(
  authenticationOptions: io.vertx.ext.hawkular.AuthenticationOptions? = null,
  batchDelay: Int? = null,
  batchSize: Int? = null,
  disabledMetricsTypes: Iterable<MetricsType>? = null,
  enabled: Boolean? = null,
  host: String? = null,
  httpHeaders: io.vertx.core.json.JsonObject? = null,
  httpOptions: io.vertx.core.http.HttpClientOptions? = null,
  metricTagsMatches: Iterable<io.vertx.ext.hawkular.MetricTagsMatch>? = null,
  metricTagsMatchs: Iterable<io.vertx.ext.hawkular.MetricTagsMatch>? = null,
  metricsBridgeAddress: String? = null,
  metricsBridgeEnabled: Boolean? = null,
  metricsServiceUri: String? = null,
  port: Int? = null,
  prefix: String? = null,
  schedule: Int? = null,
  sendTenantHeader: Boolean? = null,
  taggedMetricsCacheSize: Int? = null,
  tags: io.vertx.core.json.JsonObject? = null,
  tenant: String? = null): VertxHawkularOptions = io.vertx.ext.hawkular.VertxHawkularOptions().apply {

  if (authenticationOptions != null) {
    this.setAuthenticationOptions(authenticationOptions)
  }
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
  if (host != null) {
    this.setHost(host)
  }
  if (httpHeaders != null) {
    this.setHttpHeaders(httpHeaders)
  }
  if (httpOptions != null) {
    this.setHttpOptions(httpOptions)
  }
  if (metricTagsMatches != null) {
    this.setMetricTagsMatches(metricTagsMatches.toList())
  }
  if (metricTagsMatchs != null) {
    for (item in metricTagsMatchs) {
      this.addMetricTagsMatch(item)
    }
  }
  if (metricsBridgeAddress != null) {
    this.setMetricsBridgeAddress(metricsBridgeAddress)
  }
  if (metricsBridgeEnabled != null) {
    this.setMetricsBridgeEnabled(metricsBridgeEnabled)
  }
  if (metricsServiceUri != null) {
    this.setMetricsServiceUri(metricsServiceUri)
  }
  if (port != null) {
    this.setPort(port)
  }
  if (prefix != null) {
    this.setPrefix(prefix)
  }
  if (schedule != null) {
    this.setSchedule(schedule)
  }
  if (sendTenantHeader != null) {
    this.setSendTenantHeader(sendTenantHeader)
  }
  if (taggedMetricsCacheSize != null) {
    this.setTaggedMetricsCacheSize(taggedMetricsCacheSize)
  }
  if (tags != null) {
    this.setTags(tags)
  }
  if (tenant != null) {
    this.setTenant(tenant)
  }
}


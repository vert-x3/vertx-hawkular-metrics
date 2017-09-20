package io.vertx.kotlin.ext.metric.reporters.hawkular

import io.vertx.ext.metric.reporters.hawkular.VertxHawkularOptions
import io.vertx.core.http.HttpClientOptions
import io.vertx.ext.metric.collect.MetricsType
import io.vertx.ext.metric.reporters.hawkular.AuthenticationOptions
import io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch

/**
 * A function providing a DSL for building [io.vertx.ext.metric.reporters.hawkular.VertxHawkularOptions] objects.
 *
 * Vert.x Hawkular monitoring configuration.
 *
 * @param authenticationOptions  Set the options for authentication.
 * @param batchDelay 
 * @param batchSize 
 * @param disabledMetricsTypes 
 * @param enabled 
 * @param host  Set the Hawkular Metrics service host. Defaults to <code>localhost</code>.
 * @param httpHeaders  Set specific headers to include in HTTP requests.
 * @param httpOptions  Set the configuration of the Hawkular Metrics HTTP client.
 * @param metricTagsMatches  Sets a list of [io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch].
 * @param metricsBridgeAddress 
 * @param metricsBridgeEnabled 
 * @param metricsServiceUri  Set the Hawkular Metrics service URI. Defaults to <code>/hawkular/metrics</code>. This can be useful if you host the Hawkular server behind a proxy and manipulate the default service URI.
 * @param port  Set the Hawkular Metrics service port.  Defaults to <code>8080</code>.
 * @param prefix 
 * @param schedule 
 * @param sendTenantHeader  Set whether Hawkular tenant header should be sent. Defaults to <code>true</code>. Must be set to <code>false</code> when working with pre-Alpha13 Hawkular servers.
 * @param taggedMetricsCacheSize  Set the number of metric names to cache in order to avoid repeated tagging requests.
 * @param tags  Set tags applied to all metrics.
 * @param tenant  Set the Hawkular tenant. Defaults to <code>default</code>.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.ext.metric.reporters.hawkular.VertxHawkularOptions original] using Vert.x codegen.
 */
fun VertxHawkularOptions(
  authenticationOptions: io.vertx.ext.metric.reporters.hawkular.AuthenticationOptions? = null,
  batchDelay: Int? = null,
  batchSize: Int? = null,
  disabledMetricsTypes: Iterable<MetricsType>? = null,
  enabled: Boolean? = null,
  host: String? = null,
  httpHeaders: io.vertx.core.json.JsonObject? = null,
  httpOptions: io.vertx.core.http.HttpClientOptions? = null,
  metricTagsMatches: Iterable<io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch>? = null,
  metricsBridgeAddress: String? = null,
  metricsBridgeEnabled: Boolean? = null,
  metricsServiceUri: String? = null,
  port: Int? = null,
  prefix: String? = null,
  schedule: Int? = null,
  sendTenantHeader: Boolean? = null,
  taggedMetricsCacheSize: Int? = null,
  tags: io.vertx.core.json.JsonObject? = null,
  tenant: String? = null): VertxHawkularOptions = io.vertx.ext.metric.reporters.hawkular.VertxHawkularOptions().apply {

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


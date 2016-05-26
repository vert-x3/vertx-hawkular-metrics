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
package io.vertx.ext.hawkular;

import java.util.EnumSet;
import java.util.Set;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;

/**
 * Vert.x Hawkular monitoring configuration.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class VertxHawkularOptions extends MetricsOptions {
  /**
   * The default Hawkular server host = localhost.
   */
  public static final String DEFAULT_HOST = "localhost";

  /**
   * The default Hawkular server port = 8080.
   */
  public static final int DEFAULT_PORT = 8080;

  /**
   * The default Hawkular Metrics service URI = /hawkular/metrics.
   */
  public static final String DEFAULT_METRICS_URI = "/hawkular/metrics";

  /**
   * The default Hawkular tenant = default.
   */
  public static final String DEFAULT_TENANT = "default";

  /**
   * Default value for metric collection interval (in seconds) = 1.
   */
  public static final int DEFAULT_SCHEDULE = 1;

  /**
   * The default metric name prefix (empty).
   */
  public static final String DEFAULT_PREFIX = "";

  /**
   * Default value for the maximum number of metrics in a batch = 50.
   */
  public static final int DEFAULT_BATCH_SIZE = 50;

  /**
   * Default value for the maximum delay between two consecutive batches (in seconds) = 1.
   */
  public static final int DEFAULT_BATCH_DELAY = 1;

  /**
   * Default event bus address where applications can send business-related metrics. The metrics are sent as JSON
   * message containing at least the <code>source</code> and <code>value</code> (double) fields.
   */
  public static final String DEFAULT_METRICS_BRIDGE_ADDRESS = "hawkular.metrics";

  /**
   * The default value to enable / disable the metrics bridge. Disable by default.
   */
  public static final boolean DEFAULT_METRICS_BRIDGE_ENABLED = false;

  private String host;
  private int port;
  private HttpClientOptions httpOptions;
  private String metricsServiceUri;
  private String tenant;
  private boolean sendTenantHeader;
  private AuthenticationOptions authenticationOptions;
  private JsonObject httpHeaders;
  private int schedule;
  private String prefix;
  private int batchSize;
  private int batchDelay;
  private boolean metricsBridgeEnabled;
  private String metricsBridgeAddress;
  private Set<MetricsType> disabledMetricsTypes;

  public VertxHawkularOptions() {
    host = DEFAULT_HOST;
    port = DEFAULT_PORT;
    httpOptions = new HttpClientOptions();
    metricsServiceUri = DEFAULT_METRICS_URI;
    tenant = DEFAULT_TENANT;
    sendTenantHeader = true;
    authenticationOptions = new AuthenticationOptions();
    httpHeaders = new JsonObject();
    schedule = DEFAULT_SCHEDULE;
    prefix = DEFAULT_PREFIX;
    batchSize = DEFAULT_BATCH_SIZE;
    batchDelay = DEFAULT_BATCH_DELAY;
    metricsBridgeEnabled = DEFAULT_METRICS_BRIDGE_ENABLED;
    metricsBridgeAddress = DEFAULT_METRICS_BRIDGE_ADDRESS;
    disabledMetricsTypes = EnumSet.noneOf(MetricsType.class);
  }

  public VertxHawkularOptions(VertxHawkularOptions other) {
    super(other);
    host = other.host;
    port = other.port;
    httpOptions = other.httpOptions != null ? new HttpClientOptions(other.httpOptions) : new HttpClientOptions();
    metricsServiceUri = other.metricsServiceUri;
    tenant = other.tenant;
    sendTenantHeader = other.sendTenantHeader;
    authenticationOptions = other.authenticationOptions != null ? new AuthenticationOptions(other.authenticationOptions) : new AuthenticationOptions();
    httpHeaders = other.httpHeaders;
    schedule = other.schedule;
    prefix = other.prefix;
    batchSize = other.batchSize;
    batchDelay = other.batchDelay;
    metricsBridgeAddress = other.metricsBridgeAddress;
    metricsBridgeEnabled = other.metricsBridgeEnabled;
    disabledMetricsTypes = other.disabledMetricsTypes != null ? EnumSet.copyOf(other.disabledMetricsTypes) : EnumSet.noneOf(MetricsType.class);
  }

  public VertxHawkularOptions(JsonObject json) {
    this();
    VertxHawkularOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the Hawkular Metrics service host
   */
  public String getHost() {
    return host;
  }

  /**
   * Set the Hawkular Metrics service host. Defaults to {@code localhost}.
   */
  public VertxHawkularOptions setHost(String host) {
    this.host = host;
    return this;
  }

  /**
   * @return the Hawkular Metrics service port.
   */
  public int getPort() {
    return port;
  }

  /**
   * Set the Hawkular Metrics service port.  Defaults to {@code 8080}.
   */
  public VertxHawkularOptions setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * @return the configuration of the Hawkular Metrics HTTP client
   */
  public HttpClientOptions getHttpOptions() {
    return httpOptions;
  }

  /**
   * Set the configuration of the Hawkular Metrics HTTP client.
   */
  public VertxHawkularOptions setHttpOptions(HttpClientOptions httpOptions) {
    this.httpOptions = httpOptions;
    return this;
  }

  /**
   * @return the Hawkular Metrics service URI
   */
  public String getMetricsServiceUri() {
    return metricsServiceUri;
  }

  /**
   * Set the Hawkular Metrics service URI. Defaults to {@code /hawkular/metrics}. This can be useful if you host the
   * Hawkular server behind a proxy and manipulate the default service URI.
   */
  public VertxHawkularOptions setMetricsServiceUri(String metricsServiceUri) {
    this.metricsServiceUri = metricsServiceUri;
    return this;
  }

  /**
   * @return the Hawkular tenant
   */
  public String getTenant() {
    return tenant;
  }

  /**
   * Set the Hawkular tenant. Defaults to {@code default}.
   */
  public VertxHawkularOptions setTenant(String tenant) {
    this.tenant = tenant;
    return this;
  }

  /**
   * @return true if tenant header should be sent
   */
  public boolean isSendTenantHeader() {
    return sendTenantHeader;
  }

  /**
   * Set whether Hawkular tenant header should be sent. Defaults to {@code true}.
   * Must be set to {@code false} when working with pre-Alpha13 Hawkular servers.
   */
  public VertxHawkularOptions setSendTenantHeader(boolean sendTenantHeader) {
    this.sendTenantHeader = sendTenantHeader;
    return this;
  }

  /**
   * @return the authentication options
   */
  public AuthenticationOptions getAuthenticationOptions() {
    return authenticationOptions;
  }

  /**
   * Set the options for authentication.
   */
  public VertxHawkularOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
    this.authenticationOptions = authenticationOptions;
    return this;
  }

  /**
   * @return specific headers to include in HTTP requests
   */
  public JsonObject getHttpHeaders() {
    return httpHeaders;
  }

  /**
   * Set specific headers to include in HTTP requests.
   */
  public VertxHawkularOptions setHttpHeaders(JsonObject httpHeaders) {
    this.httpHeaders = httpHeaders;
    return this;
  }

  /**
   * @return the metric collection interval (in seconds)
   */
  public int getSchedule() {
    return schedule;
  }

  /**
   * Set the metric collection interval (in seconds). Defaults to {@code 1}.
   */
  public VertxHawkularOptions setSchedule(int schedule) {
    this.schedule = schedule;
    return this;
  }

  /**
   * @return the metric name prefix
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * Set the metric name prefix. Metric names are not prefixed by default. Prefixing metric names is required to
   * distinguish data sent by different Vert.x instances.
   */
  public VertxHawkularOptions setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * @return the maximum number of metrics in a batch
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Set the maximum number of metrics in a batch. To reduce the number of HTTP exchanges, metric data is sent to the
   * Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches the configured
   * {@code batchSize}, or after the {@code batchDelay} expires. Defaults to {@code 50}.
   */
  public VertxHawkularOptions setBatchSize(int batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  /**
   * @return the maximum delay between two consecutive batches
   */
  public int getBatchDelay() {
    return batchDelay;
  }

  /**
   * Set the maximum delay between two consecutive batches (in seconds). To reduce the number of HTTP exchanges, metric
   * data is sent to the Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches
   * the configured {@code batchSize}, or after the {@code batchDelay} expires. Defaults to {@code 1} second.
   */
  public VertxHawkularOptions setBatchDelay(int batchDelay) {
    this.batchDelay = batchDelay;
    return this;
  }

  /**
   * Set whether metrics will be enabled on the Vert.x instance. Metrics are not enabled by default.
   */
  @Override
  public VertxHawkularOptions setEnabled(boolean enable) {
    super.setEnabled(enable);
    return this;
  }

  /**
   * @return the metric bridge address. If enabled the metric bridge transfers metrics collected from the event bus to
   * the Hawkular server. The metrics are sent as message on the event bus to the return address. The message is a
   * JSON object specifying at least the {@code source} and {@code value} fields ({@code value} is a double).
   */
  public String getMetricsBridgeAddress() {
    return metricsBridgeAddress;
  }

  /**
   * Sets the metric bridge address on which the application is sending the custom metrics. Application can send
   * metrics to this event bus address. The message is a JSON object specifying at least the {@code id} and
   * {@code value} fields.
   * <p/>
   * Don't forget to also enable the bridge with {@code metricsBridgeEnabled}.
   *
   * @param metricsBridgeAddress the address
   * @return the current {@link VertxHawkularOptions} instance
   */
  public VertxHawkularOptions setMetricsBridgeAddress(String metricsBridgeAddress) {
    this.metricsBridgeAddress = metricsBridgeAddress;
    return this;
  }

  /**
   * Checks whether or not the metrics bridge is enabled.
   *
   * @return {@code true} if the metrics bridge is enabled, {@code false} otherwise.
   */
  public boolean isMetricsBridgeEnabled() {
    return metricsBridgeEnabled;
  }

  /**
   * Sets whether or not the metrics bridge should be enabled. The metrics bridge is disabled by default.
   *
   * @param metricsBridgeEnabled {@code true} to enable the bridge, {@code false} to disable it.
   * @return the current {@link VertxHawkularOptions} instance
   */
  public VertxHawkularOptions setMetricsBridgeEnabled(boolean metricsBridgeEnabled) {
    this.metricsBridgeEnabled = metricsBridgeEnabled;
    return this;
  }

  /**
   * @return the disabled metrics types.
   */
  public Set<MetricsType> getDisabledMetricsTypes() {
    return disabledMetricsTypes;
  }

  /**
   * Sets metrics types that are disabled.
   *
   * @param disabledMetricsTypes to specify the set of metrics types to be disabled.
   */
  public void setDisabledMetricsTypes(Set<MetricsType> disabledMetricsTypes) {
    this.disabledMetricsTypes = disabledMetricsTypes;
  }

  /**
   * Set metric that will not be registered. Schedulers will check the set {@code disabledMetricsTypes} when
   * registering metrics suppliers
   *
   * @param metricsType the type of metrics
   * @return the current {@link VertxHawkularOptions} instance
   */
  public VertxHawkularOptions addDisabledMetricsType(MetricsType metricsType) {
    this.disabledMetricsTypes.add(metricsType);
    return this;
  }

  public boolean isMetricsTypeDisabled(MetricsType metricsType) {
    return this.disabledMetricsTypes.contains(metricsType);
  }
}

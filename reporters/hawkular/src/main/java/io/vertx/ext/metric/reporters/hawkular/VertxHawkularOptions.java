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
package io.vertx.ext.metric.reporters.hawkular;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.metric.collect.ExtendedMetricsOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vert.x Hawkular monitoring configuration.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class VertxHawkularOptions extends ExtendedMetricsOptions {
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
   * The default value to enable / disable sending the tenant header. Enabled by default.
   */
  public static final boolean DEFAULT_SEND_TENANT_HEADER = true;

  /**
   * The default number of metric names to cache in order to avoid repeated tagging requests = 4096.
   */
  public static final int DEFAULT_TAGGED_METRICS_CACHE_SIZE = 4096;

  private String host;
  private int port;
  private HttpClientOptions httpOptions;
  private String metricsServiceUri;
  private String tenant;
  private boolean sendTenantHeader;
  private AuthenticationOptions authenticationOptions;
  private JsonObject httpHeaders;
  private JsonObject tags;
  private int taggedMetricsCacheSize;
  private List<MetricTagsMatch> metricTagsMatches;

  public VertxHawkularOptions() {
    host = DEFAULT_HOST;
    port = DEFAULT_PORT;
    httpOptions = new HttpClientOptions();
    metricsServiceUri = DEFAULT_METRICS_URI;
    tenant = DEFAULT_TENANT;
    sendTenantHeader = DEFAULT_SEND_TENANT_HEADER;
    authenticationOptions = new AuthenticationOptions();
    httpHeaders = new JsonObject();
    tags = new JsonObject();
    taggedMetricsCacheSize = DEFAULT_TAGGED_METRICS_CACHE_SIZE;
    metricTagsMatches = new ArrayList<>();
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
    tags = other.tags != null ? other.tags.copy() : new JsonObject();
    taggedMetricsCacheSize = other.taggedMetricsCacheSize;
    metricTagsMatches = new ArrayList<>(other.metricTagsMatches != null ? other.metricTagsMatches : Collections.emptyList());
  }

  public VertxHawkularOptions(JsonObject json) {
    this();
    VertxHawkularOptionsConverter.fromJson(json, this);
  }

  @Override
  public VertxHawkularOptions setEnabled(boolean enable) {
    super.setEnabled(enable);
    return this;
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
   * @return tags applied to all metrics
   */
  public JsonObject getTags() {
    return tags;
  }

  /**
   * Set tags applied to all metrics.
   */
  public VertxHawkularOptions setTags(JsonObject tags) {
    this.tags = tags;
    return this;
  }

  /**
   * @return number of metric names to cache in order to avoid repeated tagging requests
   */
  public int getTaggedMetricsCacheSize() {
    return taggedMetricsCacheSize;
  }

  /**
   * Set the number of metric names to cache in order to avoid repeated tagging requests.
   */
  public VertxHawkularOptions setTaggedMetricsCacheSize(int taggedMetricsCacheSize) {
    this.taggedMetricsCacheSize = taggedMetricsCacheSize;
    return this;
  }

  /**
   * @return the list of {@link MetricTagsMatch}
   */
  public List<MetricTagsMatch> getMetricTagsMatches() {
    return metricTagsMatches;
  }

  /**
   * Sets a list of {@link MetricTagsMatch}.
   *
   * @param metricTagsMatches a list of {@link MetricTagsMatch}
   */
  public VertxHawkularOptions setMetricTagsMatches(List<MetricTagsMatch> metricTagsMatches) {
    this.metricTagsMatches = metricTagsMatches;
    return this;
  }

  /**
   * Adds a {@link MetricTagsMatch}.
   */
  @GenIgnore
  public VertxHawkularOptions addMetricTagsMatch(MetricTagsMatch metricTagsMatch) {
    if (metricTagsMatches == null) {
      metricTagsMatches = new ArrayList<>();
    }
    metricTagsMatches.add(metricTagsMatch);
    return this;
  }
}

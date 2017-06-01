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
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.metric.collect.ExtendedMetricsOptions;
import io.vertx.ext.metric.reporters.hawkular.VertxHawkularOptionsConverter;

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

  public VertxHawkularOptions() {
    host = DEFAULT_HOST;
    port = DEFAULT_PORT;
    httpOptions = new HttpClientOptions();
    metricsServiceUri = DEFAULT_METRICS_URI;
    tenant = DEFAULT_TENANT;
    sendTenantHeader = true;
    authenticationOptions = new AuthenticationOptions();
    httpHeaders = new JsonObject();
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
}

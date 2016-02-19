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

package io.vertx.ext.hawkular;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Options specific to a standalone Metrics server.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true)
public class StandaloneMetricsOptions {
  /**
   * The default Hawkular tenant = default.
   */
  public static final String DEFAULT_TENANT = "default";

  private String tenant;

  public StandaloneMetricsOptions() {
    tenant = DEFAULT_TENANT;
  }

  public StandaloneMetricsOptions(StandaloneMetricsOptions other) {
    tenant = other.tenant;
  }

  public StandaloneMetricsOptions(JsonObject json) {
    StandaloneMetricsOptionsConverter.fromJson(json, this);
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
  public StandaloneMetricsOptions setTenant(String tenant) {
    this.tenant = tenant;
    return this;
  }
}

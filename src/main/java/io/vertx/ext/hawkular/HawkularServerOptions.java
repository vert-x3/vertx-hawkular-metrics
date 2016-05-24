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
 * Options specific to a Hawkular server.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true)
public class HawkularServerOptions {
  /**
   * The default Hawkular tenant = hawkular.
   */
  public static final String DEFAULT_TENANT = "hawkular";

  private String id;
  private String secret;
  @Deprecated
  private String persona;
  private String tenant;

  public HawkularServerOptions() {
    this.tenant = DEFAULT_TENANT;
  }

  public HawkularServerOptions(HawkularServerOptions other) {
    id = other.id;
    secret = other.secret;
    persona = other.persona;
    tenant = other.tenant;
  }

  public HawkularServerOptions(JsonObject json) {
    HawkularServerOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the identifier used for authentication
   */
  public String getId() {
    return id;
  }

  /**
   * Set the identifier used for authentication.
   */
  public HawkularServerOptions setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * @return the secret used for authentication
   */
  public String getSecret() {
    return secret;
  }

  /**
   * Set the secret used for authentication.
   */
  public HawkularServerOptions setSecret(String secret) {
    this.secret = secret;
    return this;
  }

  /**
   * @return the Hawkular Persona identifier
   * @deprecated
   * @see #setPersona(String)
   */
  @Deprecated
  public String getPersona() {
    return persona;
  }

  /**
   * DEPRECATED. Do not use with Hawkular Alpha13 and up.
   * Set the Hawkular Persona identifier. This is used to impersonate an organization with user credentials.
   *
   * @deprecated do not use with Hawkular Alpha13 and up, use {@link #setTenant(String)} instead
   */
  @Deprecated
  public HawkularServerOptions setPersona(String persona) {
    this.persona = persona;
    return this;
  }

  /**
   * @return the Hawkular tenant
   */
  public String getTenant() {
    return tenant;
  }

  /**
   * Set the Hawkular tenant. Defaults to {@code hawkular}.
   */
  public HawkularServerOptions setTenant(String tenant) {
    this.tenant = tenant;
    return this;
  }
}

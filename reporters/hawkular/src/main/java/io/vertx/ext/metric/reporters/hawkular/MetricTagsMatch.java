/*
 * Copyright 2017 Red Hat, Inc.
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
import io.vertx.core.json.JsonObject;

/**
 * Tags to apply to any metric which name matches the criteria.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true)
public class MetricTagsMatch {

  /**
   * The type of match.
   *
   * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
   */
  public enum MatchType {
    EQUALS, REGEX
  }

  /**
   * The default value for matching type = EQUALS.
   */
  public static final MatchType DEFAULT_TYPE = MatchType.EQUALS;

  /**
   * The default matching value (empty).
   */
  public static final String DEFAULT_VALUE = "";

  private MatchType type;
  private String value;
  private JsonObject tags;

  public MetricTagsMatch() {
    type = DEFAULT_TYPE;
    value = DEFAULT_VALUE;
    tags = new JsonObject();
  }

  public MetricTagsMatch(MetricTagsMatch other) {
    type = other.type;
    value = other.value;
    tags = other.tags != null ? other.tags.copy() : new JsonObject();
  }

  public MetricTagsMatch(JsonObject json) {
    this();
    MetricTagsMatchConverter.fromJson(json, this);
  }

  /**
   * @return the matcher type
   */
  public MatchType getType() {
    return type;
  }

  /**
   * Set the type of matching to apply.
   *
   * @param type the matcher type
   * @return a reference to this, so the API can be used fluently
   */
  public MetricTagsMatch setType(MatchType type) {
    this.type = type;
    return this;
  }

  /**
   * @return the matched value
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the matched value.
   *
   * @param value the value to match
   * @return a reference to this, so the API can be used fluently
   */
  public MetricTagsMatch setValue(String value) {
    this.value = value;
    return this;
  }

  /**
   * @return tags to apply if metric name matches the criteria
   */
  public JsonObject getTags() {
    return tags;
  }

  /**
   * Set the tags to apply if metric name matches the criteria.
   */
  public MetricTagsMatch setTags(JsonObject tags) {
    this.tags = tags;
    return this;
  }
}

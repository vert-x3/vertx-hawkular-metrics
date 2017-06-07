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

package io.vertx.ext.hawkular;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Tags to apply to any metric which name matches the criteria.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class MetricTagsMatch extends Match {

  private JsonObject tags;

  public MetricTagsMatch() {
    tags = new JsonObject();
  }

  public MetricTagsMatch(MetricTagsMatch other) {
    super(other);
    tags = other.tags != null ? other.tags.copy() : new JsonObject();
  }

  public MetricTagsMatch(JsonObject json) {
    this();
    MetricTagsMatchConverter.fromJson(json, this);
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

  @Override
  public String getValue() {
    return super.getValue();
  }

  @Override
  public MetricTagsMatch setValue(String value) {
    super.setValue(value);
    return this;
  }

  @Override
  public MatchType getType() {
    return super.getType();
  }

  @Override
  public MetricTagsMatch setType(MatchType type) {
    super.setType(type);
    return this;
  }
}

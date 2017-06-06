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
@DataObject(generateConverter = true)
public class MetricTagsMatch {

  private JsonObject tags;
  private Match match;

  public MetricTagsMatch() {
  }

  public MetricTagsMatch(MetricTagsMatch other) {
    tags = other.tags;
    match = other.match;
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

  /**
   * @return the criteria for metric name
   */
  public Match getMatch() {
    return match;
  }

  /**
   * Set the criteria for metric name.
   */
  public MetricTagsMatch setMatch(Match match) {
    this.match = match;
    return this;
  }
}

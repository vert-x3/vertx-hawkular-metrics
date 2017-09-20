/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.ext.metric.reporters.hawkular;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch} original class using Vert.x codegen.
 */
public class MetricTagsMatchConverter {

  public static void fromJson(JsonObject json, MetricTagsMatch obj) {
    if (json.getValue("tags") instanceof JsonObject) {
      obj.setTags(((JsonObject)json.getValue("tags")).copy());
    }
    if (json.getValue("type") instanceof String) {
      obj.setType(io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch.MatchType.valueOf((String)json.getValue("type")));
    }
    if (json.getValue("value") instanceof String) {
      obj.setValue((String)json.getValue("value"));
    }
  }

  public static void toJson(MetricTagsMatch obj, JsonObject json) {
    if (obj.getTags() != null) {
      json.put("tags", obj.getTags());
    }
    if (obj.getType() != null) {
      json.put("type", obj.getType().name());
    }
    if (obj.getValue() != null) {
      json.put("value", obj.getValue());
    }
  }
}
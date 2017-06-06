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

package io.vertx.ext.metric.collect;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.ext.metric.collect.ExtendedMetricsOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.metric.collect.ExtendedMetricsOptions} original class using Vert.x codegen.
 */
public class ExtendedMetricsOptionsConverter {

  public static void fromJson(JsonObject json, ExtendedMetricsOptions obj) {
    if (json.getValue("batchDelay") instanceof Number) {
      obj.setBatchDelay(((Number)json.getValue("batchDelay")).intValue());
    }
    if (json.getValue("batchSize") instanceof Number) {
      obj.setBatchSize(((Number)json.getValue("batchSize")).intValue());
    }
    if (json.getValue("disabledMetricsTypes") instanceof JsonArray) {
      json.getJsonArray("disabledMetricsTypes").forEach(item -> {
        if (item instanceof String)
          obj.addDisabledMetricsType(io.vertx.ext.metric.collect.MetricsType.valueOf((String)item));
      });
    }
    if (json.getValue("enabled") instanceof Boolean) {
      obj.setEnabled((Boolean)json.getValue("enabled"));
    }
    if (json.getValue("metricsBridgeAddress") instanceof String) {
      obj.setMetricsBridgeAddress((String)json.getValue("metricsBridgeAddress"));
    }
    if (json.getValue("metricsBridgeEnabled") instanceof Boolean) {
      obj.setMetricsBridgeEnabled((Boolean)json.getValue("metricsBridgeEnabled"));
    }
    if (json.getValue("prefix") instanceof String) {
      obj.setPrefix((String)json.getValue("prefix"));
    }
    if (json.getValue("schedule") instanceof Number) {
      obj.setSchedule(((Number)json.getValue("schedule")).intValue());
    }
  }

  public static void toJson(ExtendedMetricsOptions obj, JsonObject json) {
    json.put("batchDelay", obj.getBatchDelay());
    json.put("batchSize", obj.getBatchSize());
    if (obj.getDisabledMetricsTypes() != null) {
      JsonArray array = new JsonArray();
      obj.getDisabledMetricsTypes().forEach(item -> array.add(item.name()));
      json.put("disabledMetricsTypes", array);
    }
    json.put("enabled", obj.isEnabled());
    if (obj.getMetricsBridgeAddress() != null) {
      json.put("metricsBridgeAddress", obj.getMetricsBridgeAddress());
    }
    json.put("metricsBridgeEnabled", obj.isMetricsBridgeEnabled());
    if (obj.getPrefix() != null) {
      json.put("prefix", obj.getPrefix());
    }
    json.put("schedule", obj.getSchedule());
  }
}
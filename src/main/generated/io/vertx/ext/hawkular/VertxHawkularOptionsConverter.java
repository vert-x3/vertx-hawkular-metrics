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

package io.vertx.ext.hawkular;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.ext.hawkular.VertxHawkularOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.hawkular.VertxHawkularOptions} original class using Vert.x codegen.
 */
public class VertxHawkularOptionsConverter {

  public static void fromJson(JsonObject json, VertxHawkularOptions obj) {
    if (json.getValue("batchDelay") instanceof Number) {
      obj.setBatchDelay(((Number)json.getValue("batchDelay")).intValue());
    }
    if (json.getValue("batchSize") instanceof Number) {
      obj.setBatchSize(((Number)json.getValue("batchSize")).intValue());
    }
    if (json.getValue("disabledMetricsType") instanceof String) {
      obj.setDisabledMetricsType(io.vertx.ext.hawkular.MetricsTypeEnum.valueOf((String)json.getValue("disabledMetricsType")));
    }
    if (json.getValue("enabled") instanceof Boolean) {
      obj.setEnabled((Boolean)json.getValue("enabled"));
    }
    if (json.getValue("hawkularServerOptions") instanceof JsonObject) {
      obj.setHawkularServerOptions(new io.vertx.ext.hawkular.HawkularServerOptions((JsonObject)json.getValue("hawkularServerOptions")));
    }
    if (json.getValue("host") instanceof String) {
      obj.setHost((String)json.getValue("host"));
    }
    if (json.getValue("httpOptions") instanceof JsonObject) {
      obj.setHttpOptions(new io.vertx.core.http.HttpClientOptions((JsonObject)json.getValue("httpOptions")));
    }
    if (json.getValue("metricsBridgeAddress") instanceof String) {
      obj.setMetricsBridgeAddress((String)json.getValue("metricsBridgeAddress"));
    }
    if (json.getValue("metricsBridgeEnabled") instanceof Boolean) {
      obj.setMetricsBridgeEnabled((Boolean)json.getValue("metricsBridgeEnabled"));
    }
    if (json.getValue("metricsServiceUri") instanceof String) {
      obj.setMetricsServiceUri((String)json.getValue("metricsServiceUri"));
    }
    if (json.getValue("port") instanceof Number) {
      obj.setPort(((Number)json.getValue("port")).intValue());
    }
    if (json.getValue("prefix") instanceof String) {
      obj.setPrefix((String)json.getValue("prefix"));
    }
    if (json.getValue("schedule") instanceof Number) {
      obj.setSchedule(((Number)json.getValue("schedule")).intValue());
    }
    if (json.getValue("serverType") instanceof String) {
      obj.setServerType(io.vertx.ext.hawkular.ServerType.valueOf((String)json.getValue("serverType")));
    }
    if (json.getValue("standaloneMetricsOptions") instanceof JsonObject) {
      obj.setStandaloneMetricsOptions(new io.vertx.ext.hawkular.StandaloneMetricsOptions((JsonObject)json.getValue("standaloneMetricsOptions")));
    }
  }

  public static void toJson(VertxHawkularOptions obj, JsonObject json) {
    json.put("batchDelay", obj.getBatchDelay());
    json.put("batchSize", obj.getBatchSize());
    json.put("enabled", obj.isEnabled());
    if (obj.getHost() != null) {
      json.put("host", obj.getHost());
    }
    if (obj.getMetricsBridgeAddress() != null) {
      json.put("metricsBridgeAddress", obj.getMetricsBridgeAddress());
    }
    json.put("metricsBridgeEnabled", obj.isMetricsBridgeEnabled());
    if (obj.getMetricsServiceUri() != null) {
      json.put("metricsServiceUri", obj.getMetricsServiceUri());
    }
    json.put("port", obj.getPort());
    if (obj.getPrefix() != null) {
      json.put("prefix", obj.getPrefix());
    }
    json.put("schedule", obj.getSchedule());
    if (obj.getServerType() != null) {
      json.put("serverType", obj.getServerType().name());
    }
  }
}
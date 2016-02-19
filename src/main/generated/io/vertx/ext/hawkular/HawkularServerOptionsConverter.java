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
 * Converter for {@link io.vertx.ext.hawkular.HawkularServerOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.hawkular.HawkularServerOptions} original class using Vert.x codegen.
 */
public class HawkularServerOptionsConverter {

  public static void fromJson(JsonObject json, HawkularServerOptions obj) {
    if (json.getValue("id") instanceof String) {
      obj.setId((String)json.getValue("id"));
    }
    if (json.getValue("persona") instanceof String) {
      obj.setPersona((String)json.getValue("persona"));
    }
    if (json.getValue("secret") instanceof String) {
      obj.setSecret((String)json.getValue("secret"));
    }
  }

  public static void toJson(HawkularServerOptions obj, JsonObject json) {
    if (obj.getId() != null) {
      json.put("id", obj.getId());
    }
    if (obj.getPersona() != null) {
      json.put("persona", obj.getPersona());
    }
    if (obj.getSecret() != null) {
      json.put("secret", obj.getSecret());
    }
  }
}
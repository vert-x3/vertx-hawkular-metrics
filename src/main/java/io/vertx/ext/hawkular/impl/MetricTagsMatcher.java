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

package io.vertx.ext.hawkular.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.hawkular.MetricTagsMatch;
import io.vertx.ext.hawkular.MetricTagsMatch.MatchType;

import java.util.regex.Pattern;

/**
 * @author Thomas Segismont
 */
class MetricTagsMatcher {

  private final JsonObject tags;
  private final MatchType matchType;
  private final String value;
  private final Pattern pattern;

  MetricTagsMatcher(MetricTagsMatch match) {
    tags = match.getTags();
    matchType = match.getType();
    value = matchType == MatchType.EQUALS ? match.getValue() : null;
    pattern = matchType == MatchType.REGEX ? Pattern.compile(match.getValue()) : null;
  }

  boolean matches(String name) {
    switch (matchType) {
      case EQUALS:
        return name.equals(value);
      case REGEX:
        return pattern.matcher(name).matches();
      default:
        return false;
    }
  }

  JsonObject getTags() {
    return tags;
  }
}

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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Thomas Segismont
 */
class TaggedMetricsCache extends LinkedHashMap<TaggedMetricsCacheKey, Void> {

  private final int maxEntries;

  TaggedMetricsCache(int maxEntries) {
    super(10, 0.75f, true);
    this.maxEntries = maxEntries;
  }

  boolean isMetricTagged(String type, String name) {
    return containsKey(new TaggedMetricsCacheKey(type, name));
  }

  void metricTagged(String type, String name) {
    put(new TaggedMetricsCacheKey(type, name), null);
  }

  protected boolean removeEldestEntry(Map.Entry<TaggedMetricsCacheKey, Void> eldest) {
    return size() > maxEntries;
  }
}

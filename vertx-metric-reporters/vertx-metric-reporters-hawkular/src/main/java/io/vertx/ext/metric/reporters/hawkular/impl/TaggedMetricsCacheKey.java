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

package io.vertx.ext.metric.reporters.hawkular.impl;

import java.util.Objects;

/**
 * @author Thomas Segismont
 */
class TaggedMetricsCacheKey {
  private final String type;
  private final String name;

  TaggedMetricsCacheKey(String type, String name) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(name);
    this.type = type;
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TaggedMetricsCacheKey key = (TaggedMetricsCacheKey) o;
    return type.equals(key.type) && name.equals(key.name);
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }
}

/*
 * Copyright 2015 Red Hat, Inc.
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

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.metric.collect.Reporter;
import io.vertx.ext.metric.collect.impl.VertxMetricsBase;
import io.vertx.ext.metric.reporters.hawkular.VertxHawkularOptions;

/**
 * Metrics SPI implementation for Hawkular.
 *
 * @author Thomas Segismont
 * @author Dan Kristensen
 */
public class HawkularVertxMetrics extends VertxMetricsBase<VertxHawkularOptions> {

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   */
  public HawkularVertxMetrics(Vertx vertx, VertxHawkularOptions options) {
    super(vertx, options);
  }

  @Override
  public Reporter createReporter(Context context) {
    return new HawkularReporter(vertx, options, context);
  }
}

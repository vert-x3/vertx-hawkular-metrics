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
package io.vertx.ext.metric.reporters.influxdb.impl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.ext.metric.reporters.influxdb.VertxInfluxDbOptions;

/**
 * @author Dan Kristensen
 */
public class VertxMetricsFactoryImpl implements VertxMetricsFactory {
  @Override
  public VertxMetrics metrics(Vertx vertx, VertxOptions vertxOptions) {
    MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
    VertxInfluxDbOptions vertxInfluxDbOptions;
    if (metricsOptions instanceof VertxInfluxDbOptions) {
      vertxInfluxDbOptions = (VertxInfluxDbOptions) metricsOptions;
    } else {
      vertxInfluxDbOptions = new VertxInfluxDbOptions(metricsOptions.toJson());
    }
    return new VertxInfluxDbMetricsImpl(vertx, vertxInfluxDbOptions);
  }

  @Override
  public MetricsOptions newOptions() {
    return new VertxInfluxDbOptions();
  }
}

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

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.metric.collect.impl.AbstractSender;
import io.vertx.ext.metric.collect.impl.AbstractVertxMetricsImpl;
import io.vertx.ext.metric.reporters.influxdb.VertxInfluxDbOptions;

/**
 * Metrics SPI implementation.
 *
 * @author Thomas Segismont
 */
public class VertxInfluxDbMetricsImpl extends AbstractVertxMetricsImpl {

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx InfluxDb options
   */
  public VertxInfluxDbMetricsImpl(Vertx vertx, VertxInfluxDbOptions options) {
    super(vertx, options);
  }

  @Override
  public AbstractSender createSender(Context context) {
    return new InfluxDbSender(vertx, (VertxInfluxDbOptions) options, context);
  }


}

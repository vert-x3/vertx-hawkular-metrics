/*
 * Copyright (c) 2011-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.metrics.collector;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Common options for reporters sending metrics in batches.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true, inheritConverter = true)
public abstract class BatchingReporterOptions extends MetricsOptionsBase {

  /**
   * Default value for the maximum number of metrics in a batch = 50.
   */
  public static final int DEFAULT_BATCH_SIZE = 50;

  /**
   * Default value for the maximum delay between two consecutive batches (in seconds) = 1.
   */
  public static final int DEFAULT_BATCH_DELAY = 1;

  private int batchSize;
  private int batchDelay;

  public BatchingReporterOptions() {
    batchSize = DEFAULT_BATCH_SIZE;
    batchDelay = DEFAULT_BATCH_DELAY;
  }

  public BatchingReporterOptions(BatchingReporterOptions other) {
    super(other);
    batchSize = other.batchSize;
    batchDelay = other.batchDelay;
  }

  public BatchingReporterOptions(JsonObject json) {
    this();
    BatchingReporterOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the maximum number of metrics in a batch
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Set the maximum number of metrics in a batch. To reduce the number of HTTP exchanges, metric data is sent to the
   * Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches the configured
   * {@code batchSize}, or after the {@code batchDelay} expires. Defaults to {@code 50}.
   */
  public MetricsOptionsBase setBatchSize(int batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  /**
   * @return the maximum delay between two consecutive batches
   */
  public int getBatchDelay() {
    return batchDelay;
  }

  /**
   * Set the maximum delay between two consecutive batches (in seconds). To reduce the number of HTTP exchanges, metric
   * data is sent to the Hawkular server in batches. A batch is sent as soon as the number of metrics collected reaches
   * the configured {@code batchSize}, or after the {@code batchDelay} expires. Defaults to {@code 1} second.
   */
  public MetricsOptionsBase setBatchDelay(int batchDelay) {
    this.batchDelay = batchDelay;
    return this;
  }
}

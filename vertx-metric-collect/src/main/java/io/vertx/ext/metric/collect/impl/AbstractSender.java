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
package io.vertx.ext.metric.collect.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.metric.collect.ExtendedMetricsOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;

/**
 * Sends collected metrics to external uri.
 *
 * @author Thomas Segismont
 * @author Dan Kristensen
 */
public abstract class AbstractSender implements Handler<List<DataPoint>> {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractSender.class);

  private final Vertx vertx;

  private final int batchSize;
  private final long batchDelay;
  private final List<DataPoint> queue;

  private long timerId;

  private long sendTime;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Extended metrics options
   * @param context the metric collection and sending execution context
   */
  public AbstractSender(Vertx vertx, ExtendedMetricsOptions options, Context context) {
    this.vertx = vertx;
    
    batchSize = options.getBatchSize();
    batchDelay = NANOSECONDS.convert(options.getBatchDelay(), SECONDS);
    queue = new ArrayList<>(batchSize);
    context.runOnContext(aVoid -> {
      timerId = vertx.setPeriodic(MILLISECONDS.convert(batchDelay, NANOSECONDS), this::flushIfIdle);
      }
    );
    sendTime = System.nanoTime();
  }

  @Override
  public void handle(List<DataPoint> dataPoints) {
    if (LOG.isTraceEnabled()) {
      String lineSeparator = System.getProperty("line.separator");
      String msg = "Handling data points: " + lineSeparator +
        dataPoints.stream().map(DataPoint::toString).collect(joining(lineSeparator));
      LOG.trace(msg);
    }

    if (queue.size() + dataPoints.size() < batchSize) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Will queue datapoints. Queue size will be " + (queue.size()+dataPoints.size()));
      }
      queue.addAll(dataPoints);
      return;
    }
    LOG.trace("Prepare for sending datapoints");
    List<DataPoint> temp = new ArrayList<>(queue.size() + dataPoints.size());
    temp.addAll(queue);
    temp.addAll(dataPoints);
    queue.clear();
    do {
      List<DataPoint> subList = temp.subList(0, batchSize);
      send(subList);
      subList.clear();
    } while (temp.size() >= batchSize);
    queue.addAll(temp);
  }

  private void send(List<DataPoint> dataPoints) {
    Object mixedData = toMixedData(dataPoints);
    getMetricsDataUri(ar -> {
      if (ar.succeeded()) {
        sendDataTo(ar.result(), mixedData, "UTF-8");
        sendTime = System.nanoTime();
      }
    });
  }

  protected abstract void sendDataTo(String metricsDataUri, Object mixedData, String encoding);
  protected abstract void getMetricsDataUri(Handler<AsyncResult<String>> handler);
  
  protected Object toMixedData(List<DataPoint> dataPoints) { 
    Map<? extends Class<? extends DataPoint>, Map<String, List<DataPoint>>> mixedData;
    mixedData = dataPoints.stream().collect(groupingBy(DataPoint::getClass, groupingBy(DataPoint::getName)));
    
    JsonObject json = new JsonObject();
    addMixedData(json, "gauges", mixedData.get(GaugePoint.class));
    addMixedData(json, "counters", mixedData.get(CounterPoint.class));
    addMixedData(json, "availabilities", mixedData.get(AvailabilityPoint.class));
    return json.encode();
  }

  private void addMixedData(JsonObject json, String type, Map<String, List<DataPoint>> data) {
    if (data == null) {
      return;
    }

    JsonArray metrics = new JsonArray();
    data.forEach((id, points) -> {
      JsonArray jsonDataPoints = points.stream()
        .map(this::toJsonDataPoint)
        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
      metrics.add(new JsonObject().put("id", id).put("data", jsonDataPoints));
    });
    json.put(type, metrics);
  }

  private JsonObject toJsonDataPoint(DataPoint dataPoint) {
    return new JsonObject().put("timestamp", dataPoint.getTimestamp()).put("value", dataPoint.getValue());
  }

  private void flushIfIdle(Long timerId) {
    if (System.nanoTime() - sendTime > batchDelay && !queue.isEmpty()) {
      LOG.trace("Flushing queue with " + queue.size() + " elements");
      send(queue);
      queue.clear();
    }
  }

  public void stop() {
    vertx.cancelTimer(timerId);
  }
}
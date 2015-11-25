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

package verticles

import io.vertx.groovy.core.Future
import io.vertx.groovy.core.eventbus.EventBus

/**
 * @author Clement Escoffier <clement@apache.org>
 */

void vertxStart(Future startFuture) {
  def config = vertx.getOrCreateContext().config()
  def id = config['id']
  def EventBus eb = vertx.eventBus()
  def metric = [
          "id": id,
          "value" : 1.0d
  ];
  if (config["insert-timestamp"]) {
    metric.put("timestamp", System.currentTimeMillis())
  }
  if (config["counter"]) {
    metric.put("type", "counter")
    metric.put("value", 1L)
  }
  eb.send("metrics", metric)

  startFuture.complete()
}

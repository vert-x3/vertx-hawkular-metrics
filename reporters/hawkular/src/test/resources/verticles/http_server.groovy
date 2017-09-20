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

import io.vertx.core.Future

/**
 * @author Thomas Segismont
 */

void vertxStart(Future startFuture) {
  def config = vertx.getOrCreateContext().config()
  httpServer = vertx.createHttpServer()
  httpServer.websocketHandler({ ws ->
    ws.handler({ event ->
      vertx.setTimer(config.requestDelay as long, { timer ->
        ws.writeTextMessage(config.content).end();
      })
    })
  }).requestHandler({ req ->
    // Timer as artificial processing time
    vertx.setTimer(config.requestDelay as long, { handler ->
      req.response().setChunked(true).putHeader('Content-Type', 'text/plain').write(config.content as String).end()
    })
  }).listen(config.port as int, config.host as String, { res ->
    if (res.failed()) {
      startFuture.fail(res.cause().message)
    } else {
      startFuture.complete()
    }
  })
}

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
  vertx.createDatagramSocket().listen(config.port as int, config.host as String, { res ->
    if (res.failed()) {
      startFuture.fail(res.cause().message)
    } else {
      startFuture.complete()
    }
  })
}

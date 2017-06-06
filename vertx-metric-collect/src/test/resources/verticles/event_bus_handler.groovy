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
/**
 * @author Thomas Segismont
 */

void vertxStart() {
  def e = new ExpectedException()
  e.stackTrace = []
  vertx.eventBus().consumer("testSubject", { message ->
    Map body = message.body() as Map
    Thread.sleep(body.sleep as long)
    if (body.fail) {
      throw e
    }
  })
}

class ExpectedException extends RuntimeException {}
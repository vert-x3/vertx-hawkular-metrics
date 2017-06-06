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

package io.vertx.ext.metric.reporters.hawkular.impl

import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Thomas Segismont
 */
@Singleton
class TenantGenerator {
  final String TENANT_PREFIX = UUID.randomUUID().toString()
  final AtomicInteger TENANT_ID_COUNTER = new AtomicInteger(0)

  String nextTenantId() {
    return "T${TENANT_PREFIX}${TENANT_ID_COUNTER.incrementAndGet()}"
  }
}

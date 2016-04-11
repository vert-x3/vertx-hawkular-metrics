package io.vertx.ext.hawkular;

import io.vertx.codegen.annotations.VertxGen;

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

/**
 *  Metrics types for each metrics.
 */
@VertxGen
public enum MetricsTypeEnum {

    /**
     * Type for net server metrics
     */
    NET_SERVER_TYPE,
    /**
     * Type for net client metrics
     */
    NET_CLIENT_TYPE,
    /**
     * Type for http server metrics
     */
    HTTP_SERVER_TYPE,
    /**
     * Type for http client metrics
     */
    HTTP_CLIENT_TYPE,
    /**
     * Type for datagram socket metrics
     */
    DATAGRAM_SOCKET_TYPE,
    /**
     * Type for event bus metrics
     */
    EVENT_BUS_TYPE
}

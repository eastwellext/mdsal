/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.replicate.netty;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

final class MessageFrameDecoder extends LengthFieldBasedFrameDecoder {
    MessageFrameDecoder() {
        super(Constants.LENGTH_FIELD_MAX, 0, Constants.LENGTH_FIELD_LENGTH, 0, Constants.LENGTH_FIELD_LENGTH);
    }
}

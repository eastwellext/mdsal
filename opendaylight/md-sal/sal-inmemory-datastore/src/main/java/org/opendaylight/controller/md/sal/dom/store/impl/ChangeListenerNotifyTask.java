/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.util.concurrent.NotificationManager;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChangeListenerNotifyTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeListenerNotifyTask.class);

    private final Iterable<? extends DataChangeListenerRegistration<?>> listeners;
    private final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> event;

    @SuppressWarnings("rawtypes")
    private final NotificationManager<AsyncDataChangeListener,AsyncDataChangeEvent>
                                                                            notificationMgr;

    @SuppressWarnings("rawtypes")
    public ChangeListenerNotifyTask(final Iterable<? extends DataChangeListenerRegistration<?>> listeners,
            final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> event,
            final NotificationManager<AsyncDataChangeListener,AsyncDataChangeEvent> notificationMgr) {
        this.listeners = listeners;
        this.event = event;
        this.notificationMgr = notificationMgr;
    }

    @Override
    public void run() {

        for (DataChangeListenerRegistration<?> listener : listeners) {
            notificationMgr.submitNotification(listener.getInstance(), event);
        }
    }

    @Override
    public String toString() {
        return "ChangeListenerNotifyTask [listeners=" + listeners + ", event=" + event + "]";
    }
}

/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.binding.testutils;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Integration tests the DataBrokerTestModule.
 *
 * @author Michael Vorburger
 */
public class DataBrokerTestModuleTest {

    @Test
    public void ensureDataBrokerTestModuleWorksWithoutException() {
        assertNotNull(DataBrokerTestModule.dataBroker());
    }
}

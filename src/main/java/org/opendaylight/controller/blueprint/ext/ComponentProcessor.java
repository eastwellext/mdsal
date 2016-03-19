/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.ext.AbstractPropertyPlaceholder;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.util.AriesFrameworkUtil;
import org.opendaylight.controller.blueprint.BlueprintContainerRestartService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The singleton component processor that is invoked by the blueprint container to perform operations on
 * various component definitions prior to component creation.
 *
 * @author Thomas Pantelis
 */
public class ComponentProcessor implements ComponentDefinitionRegistryProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentProcessor.class);
    private static final String CM_PERSISTENT_ID_PROPERTY = "persistentId";

    private final List<ServiceRegistration<?>> managedServiceRegs = new ArrayList<>();
    private Bundle bundle;
    private BlueprintContainerRestartService blueprintContainerRestartService;
    private boolean restartDependentsOnUpdates;

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setBlueprintContainerRestartService(BlueprintContainerRestartService restartService) {
        this.blueprintContainerRestartService = restartService;
    }

    public void setRestartDependentsOnUpdates(boolean restartDependentsOnUpdates) {
        this.restartDependentsOnUpdates = restartDependentsOnUpdates;
    }

    public void destroy() {
        for(ServiceRegistration<?> reg: managedServiceRegs) {
            AriesFrameworkUtil.safeUnregisterService(reg);
        }
    }

    @Override
    public void process(ComponentDefinitionRegistry registry) {
        LOG.debug("{}: In process", bundle.getSymbolicName());

        for(String name : registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            if(component instanceof MutableBeanMetadata) {
                processMutableBeanMetadata((MutableBeanMetadata)component);
            }
        }
    }

    private void processMutableBeanMetadata(MutableBeanMetadata bean) {
        if(restartDependentsOnUpdates && bean.getRuntimeClass() != null &&
                AbstractPropertyPlaceholder.class.isAssignableFrom(bean.getRuntimeClass())) {
            LOG.debug("{}: Found PropertyPlaceholder bean: {}, runtime {}", bundle.getSymbolicName(), bean.getId(),
                    bean.getRuntimeClass());

            for(BeanProperty prop: bean.getProperties()) {
                if(CM_PERSISTENT_ID_PROPERTY.equals(prop.getName())) {
                    if(prop.getValue() instanceof ValueMetadata) {
                        ValueMetadata persistentId = (ValueMetadata)prop.getValue();

                        LOG.debug("{}: Found {} property, value : {}", bundle.getSymbolicName(),
                                CM_PERSISTENT_ID_PROPERTY, persistentId.getStringValue());

                        registerManagedService(persistentId.getStringValue());
                    } else {
                        LOG.debug("{}: {} property metadata {} is not instanceof ValueMetadata",
                                bundle.getSymbolicName(), CM_PERSISTENT_ID_PROPERTY, prop.getValue());
                    }

                    break;
                }
            }
        }
    }

    private void registerManagedService(final String persistentId) {
        // Register a ManagedService so we git updates from the ConfigAdmin when the cfg file corresponding
        // to the persistentId changes.
        ManagedService managedService = new ManagedService() {
            private volatile boolean initialUpdate = true;

            @Override
            public void updated(Dictionary<String, ?> properties) {
                LOG.debug("{}: ManagedService updated for persistentId {}, properties: {}, initialUpdate: {}",
                        bundle.getSymbolicName(), persistentId, properties, initialUpdate);

                // The first update occurs when the service is registered so ignore it as we want subsequent
                // updates when it changes. The ConfigAdmin will send an update even if the cfg file doesn't
                // yet exist.
                if(initialUpdate) {
                    initialUpdate = false;
                } else {
                    blueprintContainerRestartService.restartContainerAndDependents(bundle);
                }
            }
        };

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, persistentId);
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        managedServiceRegs.add(bundle.getBundleContext().registerService(ManagedService.class.getName(),
                managedService, props));
    }
}

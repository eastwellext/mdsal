/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.binding.dom.adapter.test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Before;
import org.opendaylight.binding.runtime.spi.GeneratedClassLoadingStrategy;
import org.opendaylight.binding.runtime.spi.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public abstract class AbstractSchemaAwareTest {
    private static final LoadingCache<Set<YangModuleInfo>, EffectiveModelContext> SCHEMA_CONTEXT_CACHE =
            CacheBuilder.newBuilder().weakValues().build(
                new CacheLoader<Set<YangModuleInfo>, EffectiveModelContext>() {
                    @Override
                    public EffectiveModelContext load(final Set<YangModuleInfo> key) {
                        return ModuleInfoBackedContext.cacheContext(
                            GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(), ImmutableSet.copyOf(key))
                                .tryToCreateModelContext().get();
                    }
                });

    @Before
    public final void setup() throws Exception {
        setupWithSchema(getSchemaContext());
    }

    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return BindingReflections.cacheModuleInfos(Thread.currentThread().getContextClassLoader());
    }

    protected EffectiveModelContext getSchemaContext() throws Exception {
        return SCHEMA_CONTEXT_CACHE.getUnchecked(getModuleInfos());
    }

    /**
     * Setups test with Schema context.
     *
     * @param context schema context
     */
    protected abstract void setupWithSchema(EffectiveModelContext context);
}

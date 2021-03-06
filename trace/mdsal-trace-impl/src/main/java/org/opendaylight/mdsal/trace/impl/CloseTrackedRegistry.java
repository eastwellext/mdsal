/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.trace.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Registry of {@link CloseTracked} instances. This class is thread-safe.
 *
 * @author Michael Vorburger.ch
 */
class CloseTrackedRegistry<T extends CloseTracked<T>> {

    private final Object anchor;
    private final String createDescription;

    private final Set<CloseTracked<T>> tracked =
        new ConcurrentSkipListSet<>(Comparator.comparingInt(System::identityHashCode));

    private final boolean isDebugContextEnabled;

    /**
     * Constructor.
     *
     * @param anchor
     *            object where this registry is stored in, used for human output in
     *            logging and other output
     * @param createDescription
     *            description of creator of instances of this registry, typically
     *            e.g. name of method in the anchor class
     * @param isDebugContextEnabled
     *            whether or not the call stack should be preserved; this is (of
     *            course) an expensive operation, and should only be used during
     *            troubleshooting
     */
    CloseTrackedRegistry(final Object anchor, final String createDescription, final boolean isDebugContextEnabled) {
        this.anchor = anchor;
        this.createDescription = createDescription;
        this.isDebugContextEnabled = isDebugContextEnabled;
    }

    public boolean isDebugContextEnabled() {
        return isDebugContextEnabled;
    }

    public Object getAnchor() {
        return anchor;
    }

    public String getCreateDescription() {
        return createDescription;
    }

    // package protected, not public; only CloseTrackedTrait invokes this
    void add(final CloseTracked<T> closeTracked) {
        tracked.add(closeTracked);
    }

    // package protected, not public; only CloseTrackedTrait invokes this
    void remove(final CloseTracked<T> closeTracked) {
        tracked.remove(closeTracked);
    }

    /**
     * Creates and returns a "report" of (currently) tracked but not (yet) closed
     * instances.
     *
     * @return Set of CloseTrackedRegistryReportEntry, of which each the stack trace
     *         element identifies a unique allocation context (or an empty List if
     *         debugContextEnabled is false), and value is the number of open
     *         instances created at that place in the code.
     */
    // For some reason, FB sees 'map' as useless but it clearly isn't.
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CloseTrackedRegistryReportEntry<T>> getAllUnique() {
        Map<List<StackTraceElement>, Long> map = new HashMap<>();
        Set<CloseTracked<T>> copyOfTracked = new HashSet<>(tracked);
        for (CloseTracked<T> closeTracked : copyOfTracked) {
            final StackTraceElement[] stackTraceArray = closeTracked.getAllocationContextStackTrace();
            List<StackTraceElement> stackTraceElements =
                    stackTraceArray != null ? Arrays.asList(stackTraceArray) : Collections.emptyList();
            map.merge(stackTraceElements, 1L, (oldValue, value) -> oldValue + 1);
        }

        Set<CloseTrackedRegistryReportEntry<T>> report = new HashSet<>();
        map.forEach((stackTraceElements, number) -> copyOfTracked.stream().filter(closeTracked -> {
            StackTraceElement[] closeTrackedStackTraceArray = closeTracked.getAllocationContextStackTrace();
            List<StackTraceElement> closeTrackedStackTraceElements =
                closeTrackedStackTraceArray != null ? asList(closeTrackedStackTraceArray) : emptyList();
            return closeTrackedStackTraceElements.equals(stackTraceElements);
        }).findAny().ifPresent(exampleCloseTracked -> report.add(
            new CloseTrackedRegistryReportEntry<>(exampleCloseTracked, number, stackTraceElements))));
        return report;
    }
}

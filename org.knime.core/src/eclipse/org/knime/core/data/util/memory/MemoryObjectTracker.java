/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 19.03.2013 by Mia
 */
package org.knime.core.data.util.memory;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.core.node.NodeLogger;

/**
 * API not public yet
 *
 * @author dietzc
 */
public class MemoryObjectTracker {

    enum Strategy {
        /* Completely frees memory */
        FREE_ALL,
        /* Free a certain percentage of all objects in the memory.*/
        FREE_PERCENTAGE,
        /* Remove oldest object (possible) */
        FREE_ONE;
    }

    /* Release Strategy */
    private Strategy m_strategy = Strategy.FREE_ALL;

    private final NodeLogger LOGGER = NodeLogger.getLogger(MemoryObjectTracker.class);

    /*
    * The list of tracked objects, whose memory will be freed, if the
    * memory runs out.
    */
    private final LinkedHashMap<Integer, WeakReference<MemoryReleasable>> TRACKED_OBJECTS =
            new LinkedHashMap<Integer, WeakReference<MemoryReleasable>>(300, 0.75f, true);

    // Singleton instance of this object
    private static MemoryObjectTracker m_instance;

    /*
    * Memory Warning System
    */
    private final MemoryWarningSystem MEMORY_WARNING_SYSTEM = MemoryWarningSystem.getInstance();

    // If true, thread can't access add/remove methods from tracker, as freeMemory is currently evaluating. avoids concurrency exception
    private boolean m_blockConcurrency = false;

    private Set<Integer> m_keysToRemove = new HashSet<Integer>();

    private Map<Integer, WeakReference<MemoryReleasable>> m_keysToAdd =
            new HashMap<Integer, WeakReference<MemoryReleasable>>();

    /*
     * Private constructor, singleton
     */
    private MemoryObjectTracker() {

        MEMORY_WARNING_SYSTEM.setPercentageUsageThreshold(0.7);

        MEMORY_WARNING_SYSTEM.registerListener(new MemoryWarningSystem.MemoryWarningListener() {

            @Override
            public void memoryUsageLow(final long usedMemory, final long maxMemory) {

                synchronized (TRACKED_OBJECTS) {
                    LOGGER.debug("low memory . used mem: " + usedMemory + ";max mem: " + maxMemory + ".");
                    switch (m_strategy) {
                        case FREE_ONE:
                            freeAllMemory(0);
                            break;
                        case FREE_ALL:
                            freeAllMemory(100);
                            break;
                        case FREE_PERCENTAGE:
                            freeAllMemory(0.5);
                            break;
                        default:
                            LOGGER.warn("Unknown MemoryObjectTracker.Strategy, using default");
                            freeAllMemory(100);
                            break;
                    }
                }
            }
        });

    }

    /**
     * Track memory releasable objects. If the memory gets low, on all tracked objects the
     * {@link MemoryReleasable#freeMemory()} method will be called and the objects itself will be removed from the
     * cache.
     *
     * @param obj
     */
    public void addMemoryReleaseable(final MemoryReleasable obj) {
        synchronized (TRACKED_OBJECTS) {
            if (m_blockConcurrency) {
                m_keysToAdd.put(obj.vmUniqueId(), new WeakReference<MemoryReleasable>(obj));
            } else {
                WeakReference<MemoryReleasable> ref = new WeakReference<MemoryReleasable>(obj);
                TRACKED_OBJECTS.put(obj.vmUniqueId(), ref);
                LOGGER.debug(TRACKED_OBJECTS.size() + " objects tracked" + " Latest Obj: " + obj.vmUniqueId());
            }
        }
    }

    public void removeMemoryReleaseable(final MemoryReleasable obj) {
        synchronized (TRACKED_OBJECTS) {
            if (m_blockConcurrency) {
                m_keysToRemove.add(obj.vmUniqueId());
            } else {
                TRACKED_OBJECTS.remove(obj.vmUniqueId());
            }
        }
    }

    /**
     * Promotes obj in LRU Cache. If obj was added to removeList before, it will be removed from removeList
     *
     * @param obj
     */
    public void promoteMemoryReleaseable(final MemoryReleasable obj) {
        synchronized (TRACKED_OBJECTS) {
            if (m_keysToRemove.contains(obj.vmUniqueId())) {
                m_keysToRemove.remove(obj.vmUniqueId());
            }

            TRACKED_OBJECTS.get(obj.vmUniqueId());
        }
    }

    /**
     * Heuristic to make sure that memory is available for a given object TODO: This is not working yet.
     *
     * @param allocator
     * @return
     */
    public <T> T safeInstantiation(final MemoryAllocator<T> allocator) {
        synchronized (TRACKED_OBJECTS) {
            freeAllMemory(100);
            return allocator.allocate();
        }
    }

    /*
    * Frees the memory of all objects in the list.
    *
    * TODO: Are there race conditions if freeMemory is called and at the
    * same time some performs a null check on getData in a cell?!
    *
    * "Old" objects are removed first (LRU fashion, accessOrder on LinkedHashMap)
    */
    private void freeAllMemory(final double percentage) {
        synchronized (TRACKED_OBJECTS) {

            m_blockConcurrency = true;
            double initSize = TRACKED_OBJECTS.size();
            int count = 0;

            for (Entry<Integer, WeakReference<MemoryReleasable>> entry : TRACKED_OBJECTS.entrySet()) {
                MemoryReleasable memoryReleasable = entry.getValue().get();
                if (memoryReleasable != null) {
                    if (memoryReleasable.memoryAlert()) {
                        m_keysToRemove.add(entry.getKey());
                        count++;
                    }
                }
                if (count / initSize >= percentage) {
                    break;
                }
            }
            m_blockConcurrency = false;

            // Remove keys from tracker
            for (Integer key : m_keysToRemove) {
                TRACKED_OBJECTS.remove(key);
            }

            // Add blocked keys
            for (Entry<Integer, WeakReference<MemoryReleasable>> entry : m_keysToAdd.entrySet()) {
                TRACKED_OBJECTS.put(entry.getKey(), entry.getValue());
            }

            m_keysToAdd.clear();
            m_keysToRemove.clear();

            LOGGER.debug(count + " tracked objects have been released.");
        }
    }

    /**
     * Singleton on MemoryObjectTracker
     */
    public static MemoryObjectTracker getInstance() {
        if (m_instance == null) {
            m_instance = new MemoryObjectTracker();
        }
        return m_instance;
    }
}

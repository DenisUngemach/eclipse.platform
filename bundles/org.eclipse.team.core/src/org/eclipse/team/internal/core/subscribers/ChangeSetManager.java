/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.runtime.*;

/**
 * An abstract class that managers a collection of change sets.
 */
public abstract class ChangeSetManager {

    private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);
    private Set sets;
	private boolean initializing;
    
    /**
     * Return the list of listeners registered with this change set manager.
     * @return the list of listeners registered with this change set manager
     */
    protected Object[] getListeners() {
        return listeners.getListeners();
    }
    
    /**
     * Method that can be invoked by subclasses when the name of
     * a managed change set changes.
     * @param set the set whose title has changed
     */
    protected void fireNameChangedEvent(final ChangeSet set) {
    	if (initializing)
    		return;
        if (contains(set)) {
            Object[] listeners = getListeners();
            for (int i = 0; i < listeners.length; i++) {
                final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
                SafeRunner.run(new ISafeRunnable() {
                    public void handleException(Throwable exception) {
                        // Exceptions are logged by the platform
                    }
                    public void run() throws Exception {
                        listener.nameChanged(set);
                    }
                });
            }
        }
    }
    
    /**
     * Method which allows subclasses to notify listeners that the default
     * set has changed.
     * @param oldSet the previous default
     * @param defaultSet the new default
     */
    protected void fireDefaultChangedEvent(final ChangeSet oldSet, final ChangeSet defaultSet) {
    	if (initializing)
    		return;
        Object[] listeners = getListeners();
        for (int i = 0; i < listeners.length; i++) {
            final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
            SafeRunner.run(new ISafeRunnable() {
                public void handleException(Throwable exception) {
                    // Exceptions are logged by the platform
                }
                public void run() throws Exception {
                    listener.defaultSetChanged(oldSet, defaultSet);
                }
            });
        }
    }
    
    /**
     * Add the set to the list of active sets.
     * @param set the set to be added
     */
    public void add(final ChangeSet set) {
        if (!contains(set)) {
        	internalGetSets().add(set);
            handleSetAdded(set);
        }
    }

    /**
     * Handle the set addition by notifying listeners.
     * @param set the added set
     */
	protected void handleSetAdded(final ChangeSet set) {
    	if (initializing)
    		return;
		Object[] listeners = getListeners();
		for (int i = 0; i < listeners.length; i++) {
		    final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
		    SafeRunner.run(new ISafeRunnable() {
		        public void handleException(Throwable exception) {
		            // Exceptions are logged by the platform
		        }
		        public void run() throws Exception {
		            listener.setAdded(set);
		        }
		    });
		}
	}

    /**
     * Remove the set from the list of active sets.
     * @param set the set to be removed
     */
    public void remove(final ChangeSet set) {
        if (contains(set)) {
        	internalGetSets().remove(set);
            handleSetRemoved(set);
        }
    }

    /**
     * Handle the set removal by notifying listeners.
     * @param set the removed set
     */
	protected void handleSetRemoved(final ChangeSet set) {
    	if (initializing)
    		return;
		Object[] listeners = getListeners();
		for (int i = 0; i < listeners.length; i++) {
		    final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
		    SafeRunner.run(new ISafeRunnable() {
		        public void handleException(Throwable exception) {
		            // Exceptions are logged by the platform
		        }
		        public void run() throws Exception {
		            listener.setRemoved(set);
		        }
		    });
		}
	}

    /**
     * Return whether the manager contains the given commit set
     * @param set the commit set being tested
     * @return whether the set is contained in the manager's list of active sets
     */
    public boolean contains(ChangeSet set) {
        return internalGetSets().contains(set);
    }

    /**
     * Add the listener to the set of registered listeners.
     * @param listener the listener to be added
     */
    public void addListener(IChangeSetChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the listener from the set of registered listeners.
     * @param listener the listener to remove
     */
    public void removeListener(IChangeSetChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Return the list of active commit sets.
     * @return the list of active commit sets
     */
    public ChangeSet[] getSets() {
        Set sets = internalGetSets();
		return (ChangeSet[]) sets.toArray(new ChangeSet[sets.size()]);
    }
    
    /**
     * Dispose of any resources maintained by the manager
     */
    public void dispose() {
        // Nothing to do
    }

    /**
     * Fire resource change notifications to the listeners.
     * @param changeSet
     * @param allAffectedResources
     */
    protected void fireResourcesChangedEvent(final ChangeSet changeSet, final IPath[] allAffectedResources) {
    	if (initializing)
    		return;
        Object[] listeners = getListeners();
        for (int i = 0; i < listeners.length; i++) {
            final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
            SafeRunner.run(new ISafeRunnable() {
                public void handleException(Throwable exception) {
                    // Exceptions are logged by the platform
                }
                public void run() throws Exception {
                    listener.resourcesChanged(changeSet, allAffectedResources);
                }
            });
        }
    }
    
    private Set internalGetSets() {
    	if (sets == null) {
    		sets = Collections.synchronizedSet(new HashSet());
    		try {
    			initializing = true;
    			initializeSets();
    		} finally {
    			initializing = false;
    		}
    	}
    	return sets;
    }

    /**
     * Initialize the sets contained in this manager.
     * This method is called the first time the sets are accessed.
     */
	protected abstract void initializeSets();
	
	public boolean isInitialized() {
		return sets != null;
	}
}

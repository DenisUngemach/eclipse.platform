/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.sets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ITeamResourceChangeListener;
import org.eclipse.team.core.subscribers.TeamDelta;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;
import org.eclipse.ui.IWorkingSet;

/**
 * SubscriberInput encapsulates the UI model for synchronization changes associated
 * with a TeamSubscriber. 
 */
public class SubscriberInput implements IPropertyChangeListener, ITeamResourceChangeListener, IResourceChangeListener {

	/*
	 * The subscriberInput manages a sync set that contains all of the out-of-sync elements
	 * of a subscriber.  
	 */
	private SyncSetInputFromSubscriber subscriberSyncSet;
	
	/*
	 * The working set sync set is used to constrain the subscriber's resources to 
	 * a smaller workset.  
	 */
	private WorkingSetSyncSetInput workingRootsSet;
	
	/*
	 * The filtered set contains the changes after direction and kind filters have been applied
	 */
	private SyncSetInputFromSyncSet filteredSyncSet;
	
	/*
	 * Responsible for calculating changes to a set based on events generated
	 * in the workbench.
	 */
	private SubscriberEventHandler eventHandler;

	private TeamSubscriberParticipant participant;
	
	public SubscriberInput(TeamSubscriberParticipant participant, TeamSubscriber subscriber) {
		this.participant = participant;
		Assert.isNotNull(subscriber);		
		subscriberSyncSet = new SyncSetInputFromSubscriber(subscriber);
		workingRootsSet = new WorkingSetSyncSetInput(subscriberSyncSet.getSyncSet());
		filteredSyncSet = new SyncSetInputFromSyncSet(workingRootsSet.getSyncSet());
		eventHandler = new SubscriberEventHandler(subscriberSyncSet);
		
		TeamUI.addPropertyChangeListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		subscriber.addListener(this);
	}
	
	public TeamSubscriber getSubscriber() {
		return subscriberSyncSet.getSubscriber();
	}
	
	public TeamSubscriberParticipant getParticipant() {
		return participant;
	}
	
	public SyncSet getFilteredSyncSet() {
		return filteredSyncSet.getSyncSet();
	}
	
	public SyncSet getSubscriberSyncSet() {
		return subscriberSyncSet.getSyncSet();
	}
	
	public SyncSet getWorkingSetSyncSet() {
		return workingRootsSet.getSyncSet();
	}

	public SubscriberEventHandler getEventHandler() {
		return eventHandler;
	}

	public void setFilter(SyncInfoFilter filter, IProgressMonitor monitor) throws TeamException {
		filteredSyncSet.setFilter(filter);
		filteredSyncSet.reset(monitor);
	}
	
	public void setWorkingSet(IWorkingSet set) {
		workingRootsSet.setWorkingSet(set);
	}

	public IWorkingSet getWorkingSet() {
		return workingRootsSet.getWorkingSet();
	}

	public void dispose() {
		eventHandler.shutdown();

		filteredSyncSet.disconnect();		
		workingRootsSet.disconnect();
		subscriberSyncSet.disconnect();		
				
		getSubscriber().removeListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);		
		TeamUI.removePropertyChangeListener(this);		
	}
	
	public IResource[] workingSetRoots() {
		return workingRootsSet.roots(getSubscriber());
	}

	public IResource[] subscriberRoots() {
		return getSubscriber().roots();
	}

	/* (non-Javadoc)
	 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TeamUI.GLOBAL_IGNORES_CHANGED)) {
			try {
				reset();
			} catch (TeamException e) {
				TeamUIPlugin.log(e);
			}
		}
	}

	public void reset() throws TeamException {
		subscriberSyncSet.reset(new NullProgressMonitor());		
		workingRootsSet.reset(new NullProgressMonitor());
		filteredSyncSet.reset(new NullProgressMonitor());		
		eventHandler.initialize();
	}

	/**
	 * Process the resource delta
	 * 
	 * @param delta
	 */
	private void processDelta(IResourceDelta delta) {
		IResource resource = delta.getResource();
		int kind = delta.getKind();
		
		if (resource.getType() == IResource.PROJECT) {
			// Handle a deleted project	
			if (((kind & IResourceDelta.REMOVED) != 0)) {
				eventHandler.remove(resource);
				return;
			}
			// Handle a closed project
			if ((delta.getFlags() & IResourceDelta.OPEN) != 0 && !((IProject)resource).isOpen()) {
				eventHandler.remove(resource);
				return;
			}	
			// Only interested in projects mapped to the provider
			if (!isVisibleProject((IProject)resource)) {
				return;
			}
		}
		
		// If the resource has changed type, remove the old resource handle
		// and add the new one
		if ((delta.getFlags() & IResourceDelta.TYPE) != 0) {
			eventHandler.remove(resource);			
			eventHandler.change(resource, IResource.DEPTH_INFINITE);
		}
		
		// Check the flags for changes the SyncSet cares about.
		// Notice we don't care about MARKERS currently.
		int changeFlags = delta.getFlags();
		if ((changeFlags	& (IResourceDelta.OPEN | IResourceDelta.CONTENT)) != 0) {
				eventHandler.change(resource, IResource.DEPTH_ZERO);
		}
		
		// Check the kind and deal with those we care about
		if ((delta.getKind() & (IResourceDelta.REMOVED | IResourceDelta.ADDED)) != 0) {
			eventHandler.change(resource, IResource.DEPTH_ZERO);
		}
		
		// Handle changed children .
		IResourceDelta[] affectedChildren =
				delta.getAffectedChildren(IResourceDelta.CHANGED | IResourceDelta.REMOVED | IResourceDelta.ADDED);
		for (int i = 0; i < affectedChildren.length; i++) {
			processDelta(affectedChildren[i]);
		}		
	}

	private boolean isVisibleProject(IProject project) {
		IResource[] roots = getSubscriber().roots();
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			if (project.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		processDelta(event.getDelta());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ITeamResourceChangeListener#teamResourceChanged(org.eclipse.team.core.sync.TeamDelta[])
	 */
	public void teamResourceChanged(TeamDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			switch(deltas[i].getFlags()) {
				case TeamDelta.SYNC_CHANGED:
					eventHandler.change(deltas[i].getResource(), IResource.DEPTH_ZERO);
					break;
				case TeamDelta.PROVIDER_DECONFIGURED:
					eventHandler.remove(deltas[i].getResource());
					break;
				case TeamDelta.PROVIDER_CONFIGURED:
					eventHandler.change(deltas[i].getResource(), IResource.DEPTH_INFINITE);
					break; 						
			}
		}
	}

	public void registerListeners(ISyncSetChangedListener listener) {
		getWorkingSetSyncSet().addSyncSetChangedListener(listener);
		getFilteredSyncSet().addSyncSetChangedListener(listener);
		getSubscriberSyncSet().addSyncSetChangedListener(listener);
	}

	public void deregisterListeners(ISyncSetChangedListener listener) {
		getWorkingSetSyncSet().removeSyncSetChangedListener(listener);
		getFilteredSyncSet().removeSyncSetChangedListener(listener);
		getSubscriberSyncSet().removeSyncSetChangedListener(listener);
	}
}

/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Utilities for resource tests.
 */
public final class ResourceTestUtil {
	private ResourceTestUtil() {
	}

	/**
	 * Assert whether or not the given resource exists in the workspace resource
	 * info tree.
	 */
	public static void assertExistsInWorkspace(IResource resource) {
		assertTrue(resource.getFullPath() + " unexpectedly does not exist in the workspace",
				existsInWorkspace(resource));
	}

	/**
	 * Assert that each element of the resource array exists in the workspace
	 * resource info tree.
	 */
	public static void assertExistsInWorkspace(IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInWorkspace(resource);
		}
	}

	private static boolean existsInWorkspace(IResource resource) {
		class CheckIfResourceExistsJob extends Job {
			private final AtomicBoolean resourceExists = new AtomicBoolean(false);

			public CheckIfResourceExistsJob() {
				super("Checking whether resource exists: " + resource);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				IResource target = getWorkspace().getRoot().findMember(resource.getFullPath(), false);
				boolean existsInWorkspace = target != null && target.getType() == resource.getType();
				resourceExists.set(existsInWorkspace);

				return Status.OK_STATUS;
			}

			boolean resourceExists() {
				return resourceExists.get();
			}
		}

		IWorkspace workspace = getWorkspace();
		ISchedulingRule modifyWorkspaceRule = workspace.getRuleFactory().modifyRule(workspace.getRoot());

		CheckIfResourceExistsJob checkIfResourceExistsJob = new CheckIfResourceExistsJob();
		checkIfResourceExistsJob.setRule(modifyWorkspaceRule);
		checkIfResourceExistsJob.schedule();
		try {
			checkIfResourceExistsJob.join(30_000, new NullProgressMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			throw new IllegalStateException("failed when joining resource-existence-checking job", e);
		}
		return checkIfResourceExistsJob.resourceExists();
	}

	/**
	 * Assert that the given resource does not exist in the workspace resource info
	 * tree.
	 */
	public static void assertDoesNotExistInWorkspace(IResource resource) {
		assertFalse(resource.getFullPath() + " unexpectedly exists in the workspace", existsInWorkspace(resource));
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * workspace resource info tree.
	 */
	public static void assertDoesNotExistInWorkspace(IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInWorkspace(resource);
		}
	}

	/**
	 * Assert whether or not the given resource exists in the local store. Use the
	 * resource manager to ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void assertExistsInFileSystem(IResource resource) {
		assertTrue(resource.getFullPath() + " unexpectedly does not exist in the file system",
				existsInFileSystem(resource));
	}


	/**
	 * Assert that each element in the resource array exists in the local store.
	 */
	public static void assertExistsInFileSystem(IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInFileSystem(resource);
		}
	}

	private static boolean existsInFileSystem(IResource resource) {
		IPath path = resource.getLocation();
		if (path == null) {
			path = computeDefaultLocation(resource);
		}
		return path.toFile().exists();
	}

	private static IPath computeDefaultLocation(IResource target) {
		switch (target.getType()) {
		case IResource.ROOT:
			return Platform.getLocation();
		case IResource.PROJECT:
			return Platform.getLocation().append(target.getFullPath());
		default:
			IPath location = computeDefaultLocation(target.getProject());
			location = location.append(target.getFullPath().removeFirstSegments(1));
			return location;
		}
	}

	/**
	 * Assert that the given resource does not exist in the local store.
	 */
	public static void assertDoesNotExistInFileSystem(IResource resource) {
		assertFalse(resource.getFullPath() + " unexpectedly exists in the file system", existsInFileSystem(resource));
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * local store.
	 */
	public static void assertDoesNotExistInFileSystem(IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInFileSystem(resource);
		}
	}

}
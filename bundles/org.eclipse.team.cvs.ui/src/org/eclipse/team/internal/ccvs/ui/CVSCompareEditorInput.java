/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * A compare input for comparing remote resources. Use <code>CVSLocalCompareInput</code> 
 * when comparing resources in the workspace to remote resources.
 */
public class CVSCompareEditorInput extends CompareEditorInput {
	private ITypedElement left;
	private ITypedElement right;
	private ITypedElement ancestor;	
	private Image leftImage;
	private Image rightImage;
	private Image ancestorImage;
	
	// comparison constants
	private static final int NODE_EQUAL = 0;
	private static final int NODE_NOT_EQUAL = 1;
	private static final int NODE_UNKNOWN = 2;
	
	String toolTipText;
    private String title;
	
	/**
	 * Creates a new CVSCompareEditorInput.
	 */
	public CVSCompareEditorInput(ResourceEditionNode left, ResourceEditionNode right) {
		this(left, right, null);
	}
	
	public CVSCompareEditorInput(String title, String toolTip, ResourceEditionNode left, ResourceEditionNode right) {
		this(left, right, null);
		this.title = title;
		this.toolTipText = toolTip;
	}
	
	/**
	 * Creates a new CVSCompareEditorInput.
	 */
	public CVSCompareEditorInput(ResourceEditionNode left, ResourceEditionNode right, ResourceEditionNode ancestor) {
		super(new CompareConfiguration());
		// TODO: Invokers of this method should ensure that trees and contents are prefetched
		this.left = left;
		this.right = right;
		this.ancestor = ancestor;
		if (left != null) {
			this.leftImage = left.getImage();
		}
		if (right != null) {
			this.rightImage = right.getImage();
		}
		if (ancestor != null) {
			this.ancestorImage = ancestor.getImage();
		}
	}
	
	/**
	 * Returns the label for the given input element.
	 */
	private String getLabel(ITypedElement element) {
		if (element instanceof ResourceEditionNode) {
			ICVSRemoteResource edition = ((ResourceEditionNode)element).getRemoteResource();
			ICVSResource resource = edition;
			if (edition instanceof ICVSRemoteFile) {
				try {
					return Policy.bind("nameAndRevision", resource.getName(), ((ICVSRemoteFile)edition).getRevision()); //$NON-NLS-1$
				} catch (TeamException e) {
					// fall through
				}
			}
			try {
				if (edition.isContainer()) {
					CVSTag tag = ((ICVSRemoteFolder)edition).getTag();
					if (tag == null) {
						return Policy.bind("CVSCompareEditorInput.inHead", edition.getName()); //$NON-NLS-1$
					} else if (tag.getType() == CVSTag.BRANCH) {
						return Policy.bind("CVSCompareEditorInput.inBranch", new Object[] {edition.getName(), tag.getName()}); //$NON-NLS-1$
					} else {
						return Policy.bind("CVSCompareEditorInput.repository", new Object[] {edition.getName(), tag.getName()}); //$NON-NLS-1$
					}
				} else {
					return Policy.bind("CVSCompareEditorInput.repository", new Object[] {edition.getName(), resource.getSyncInfo().getRevision()}); //$NON-NLS-1$
				}
			} catch (TeamException e) {
				handle(e);
				// Fall through and get the default label
			}
		}
		return element.getName();
	}
	
	/**
	 * Returns the label for the given input element.
	 */
	private String getVersionLabel(ITypedElement element) {
		if (element instanceof ResourceEditionNode) {
			ICVSRemoteResource edition = ((ResourceEditionNode)element).getRemoteResource();
			ICVSResource resource = edition;
			try {
				if (edition.isContainer()) {
					CVSTag tag = ((ICVSRemoteFolder)resource).getTag();
					if (tag == null) {
						return Policy.bind("CVSCompareEditorInput.headLabel"); //$NON-NLS-1$
					} else if (tag.getType() == CVSTag.BRANCH) {
						return Policy.bind("CVSCompareEditorInput.branchLabel", tag.getName()); //$NON-NLS-1$
					} else {
						return tag.getName();
					}
				} else {
					return resource.getSyncInfo().getRevision();
				}
			} catch (TeamException e) {
				handle(e);
				// Fall through and get the default label
			}
		}
		return element.getName();
	}
		
	/*
	 * Returns a guess of the resource name being compared, for display
	 * in the title.
	 */
	private String guessResourceName() {
		if (left != null) {
			return left.getName();
		}
		if (right != null) {
			return right.getName();
		}
		if (ancestor != null) {
			return ancestor.getName();
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * Returns a guess of the resource path being compared, for display
	 * in the tooltip.
	 */
	private Object guessResourcePath() {
		if (left != null && left instanceof ResourceEditionNode) {
			return ((ResourceEditionNode)left).getRemoteResource().getRepositoryRelativePath();
		}
		if (right != null && right instanceof ResourceEditionNode) {
			return ((ResourceEditionNode)right).getRemoteResource().getRepositoryRelativePath();
		}
		if (ancestor != null && ancestor instanceof ResourceEditionNode) {
			return ((ResourceEditionNode)ancestor).getRemoteResource().getRepositoryRelativePath();
		}
		return guessResourceName();
	}
	
	/**
	 * Handles a random exception and sanitizes it into a reasonable
	 * error message.  
	 */
	private void handle(Exception e) {
		// create a status
		Throwable t = e;
		// unwrap the invocation target exception
		if (t instanceof InvocationTargetException) {
			t = ((InvocationTargetException)t).getTargetException();
		}
		IStatus error;
		if (t instanceof CoreException) {
			error = ((CoreException)t).getStatus();
		} else if (t instanceof TeamException) {
			error = ((TeamException)t).getStatus();
		} else {
			error = new Status(IStatus.ERROR, CVSUIPlugin.ID, 1, Policy.bind("internal"), t); //$NON-NLS-1$
		}
		setMessage(error.getMessage());
		if (!(t instanceof TeamException)) {
			CVSUIPlugin.log(error.getSeverity(), error.getMessage(), t);
		}
	}
	
	/**
	 * Sets up the title and pane labels for the comparison view.
	 */
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		setLabels(cc, new StructuredSelection());
		
		if (title == null) {
			if (ancestor != null) {
				title = Policy.bind("CVSCompareEditorInput.titleAncestor", new Object[] {guessResourceName(), getVersionLabel(ancestor), getVersionLabel(left), getVersionLabel(right)} ); //$NON-NLS-1$
				toolTipText = Policy.bind("CVSCompareEditorInput.titleAncestor", new Object[] {guessResourcePath(), getVersionLabel(ancestor), getVersionLabel(left), getVersionLabel(right)} ); //$NON-NLS-1$
			} else {
				String leftName = null;
				if (left != null) leftName = left.getName();
				String rightName = null;
				if (right != null) rightName = right.getName();
				if (leftName != null && !leftName.equals(rightName)) {
					title = Policy.bind("CVSCompareEditorInput.titleNoAncestorDifferent", new Object[] {leftName, getVersionLabel(left), rightName, getVersionLabel(right)} );  //$NON-NLS-1$
				} else {
					title = Policy.bind("CVSCompareEditorInput.titleNoAncestor", new Object[] {guessResourceName(), getVersionLabel(left), getVersionLabel(right)} ); //$NON-NLS-1$
					title = Policy.bind("CVSCompareEditorInput.titleNoAncestor", new Object[] {guessResourcePath(), getVersionLabel(left), getVersionLabel(right)} ); //$NON-NLS-1$
				}
			}
		}
		setTitle(title);
	}

	private void setLabels(CompareConfiguration cc, IStructuredSelection selection) {
		ITypedElement left = this.left;
		ITypedElement right = this.right;
		ITypedElement ancestor = this.ancestor;
		
		if (left != null) {
			cc.setLeftLabel(getLabel(left));
			cc.setLeftImage(leftImage);
		}
	
		if (right != null) {
			cc.setRightLabel(getLabel(right));
			cc.setRightImage(rightImage);
		}
		
		if (ancestor != null) {
			cc.setAncestorLabel(getLabel(ancestor));
			cc.setAncestorImage(ancestorImage);
		}
	}
	
	/* (Non-javadoc)
	 * Method declared on CompareEditorInput
	 */
	public boolean isSaveNeeded() {
		return false;
	}

	/* (non-Javadoc)
	 * Method declared on CompareEditorInput
	 */
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		final boolean threeWay = ancestor != null;
		if (right == null || left == null) {
			setMessage(Policy.bind("CVSCompareEditorInput.different")); //$NON-NLS-1$
			return null;
		}
		
		initLabels();
	
		final Differencer d = new Differencer() {
			protected boolean contentsEqual(Object input1, Object input2) {
				int compare = teamEqual(input1, input2);
				if (compare == NODE_EQUAL) {
					return true;
				}
				if (compare == NODE_NOT_EQUAL) {
					return false;
				}
				//revert to slow content comparison
				return super.contentsEqual(input1, input2);
			}
			protected void updateProgress(IProgressMonitor progressMonitor, Object node) {
				if (node instanceof ITypedElement) {
					ITypedElement element = (ITypedElement)node;
					progressMonitor.subTask(Policy.bind("CompareEditorInput.fileProgress", new String[] {element.getName()})); //$NON-NLS-1$
					progressMonitor.worked(1);
				}
			}
			protected Object[] getChildren(Object input) {
				if (input instanceof IStructureComparator) {
					Object[] children= ((IStructureComparator)input).getChildren();
					if (children != null)
						return children;
				}
				return null;
			}
			protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
				return new DiffNode((IDiffContainer) data, result, (ITypedElement)ancestor, (ITypedElement)left, (ITypedElement)right);
			}
		};
		
		try {	
			// do the diff	
			Object result = null;
			monitor.beginTask(Policy.bind("CVSCompareEditorInput.comparing"), 30); //$NON-NLS-1$
			IProgressMonitor sub = new SubProgressMonitor(monitor, 30);
			sub.beginTask(Policy.bind("CVSCompareEditorInput.comparing"), 100); //$NON-NLS-1$
			try {
				result = d.findDifferences(threeWay, sub, null, ancestor, left, right);
			} finally {
				sub.done();
			}
			return result;
		} catch (OperationCanceledException e) {
			throw new InterruptedException(e.getMessage());
		} catch (RuntimeException e) {
			handle(e);
			return null;
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Compares two nodes to determine if they are equal.  Returns NODE_EQUAL
	 * of they are the same, NODE_NOT_EQUAL if they are different, and
	 * NODE_UNKNOWN if comparison was not possible.
	 */
	protected int teamEqual(Object left, Object right) {
		// calculate the type for the left contribution
		ICVSRemoteResource leftEdition = null;
		if (left instanceof ResourceEditionNode) {
			leftEdition = ((ResourceEditionNode)left).getRemoteResource();
		}
		
		// calculate the type for the right contribution
		ICVSRemoteResource rightEdition = null;
		if (right instanceof ResourceEditionNode)
			rightEdition = ((ResourceEditionNode)right).getRemoteResource();
		
		
		// compare them
			
		if (leftEdition == null || rightEdition == null) {
			return NODE_UNKNOWN;
		}
		// if they're both non-files, they're the same
		if (leftEdition.isContainer() && rightEdition.isContainer()) {
			return NODE_EQUAL;
		}
		// if they have different types, they're different
		if (leftEdition.isContainer() != rightEdition.isContainer()) {
			return NODE_NOT_EQUAL;
		}
		
		String leftLocation = leftEdition.getRepository().getLocation();
		String rightLocation = rightEdition.getRepository().getLocation();
		if (!leftLocation.equals(rightLocation)) {
			return NODE_UNKNOWN;
		}
		try {
			ResourceSyncInfo leftInfo = ((ICVSResource)leftEdition).getSyncInfo();
			ResourceSyncInfo rightInfo = ((ICVSResource)rightEdition).getSyncInfo();
			
			if (leftEdition.getRepositoryRelativePath().equals(rightEdition.getRepositoryRelativePath()) &&
				leftInfo.getRevision().equals(rightInfo.getRevision())) {
				return NODE_EQUAL;
			} else {
				if(considerContentIfRevisionOrPathDiffers()) {
					return NODE_UNKNOWN;
				} else {
					return NODE_NOT_EQUAL;
				}
			}
		} catch (TeamException e) {
			handle(e);
			return NODE_UNKNOWN;
		}
	}
	
	private boolean considerContentIfRevisionOrPathDiffers() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_CONSIDER_CONTENTS);
	}
	
	public Viewer createDiffViewer(Composite parent) {
		Viewer viewer = super.createDiffViewer(parent);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				CompareConfiguration cc = getCompareConfiguration();
				setLabels(cc, (IStructuredSelection)event.getSelection());
			}
		});
		((StructuredViewer)viewer).addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                ISelection selection = event.getSelection();
                if (! selection.isEmpty() && selection instanceof IStructuredSelection) {
                    Object o = ((IStructuredSelection)selection).getFirstElement();
                    if (o instanceof DiffNode) {
                        updateLabelsFor((DiffNode)o);
                    }
                }
            }
        });
		return viewer;
	}
	
	/*
	 * Update the labels for the given DiffNode
     */
    protected void updateLabelsFor(DiffNode node) {
        CompareConfiguration cc = getCompareConfiguration();
        ITypedElement l = node.getLeft();
        if (l == null) {
            cc.setLeftLabel(Policy.bind("CVSCompareEditorInput.0")); //$NON-NLS-1$
            cc.setLeftImage(null);
        } else {
	        cc.setLeftLabel(getLabel(l));
	        cc.setLeftImage(l.getImage());
        }
        ITypedElement r = node.getRight();
        if (r == null) {
            cc.setRightLabel(Policy.bind("CVSCompareEditorInput.1")); //$NON-NLS-1$
            cc.setRightImage(null);
        } else {
	        cc.setRightLabel(getLabel(r));
	        cc.setRightImage(r.getImage());
        }
    }

    /* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		if (toolTipText != null) {
			return toolTipText;
		}
		return super.getToolTipText();
	}
	
}

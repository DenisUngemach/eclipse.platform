/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup.browsers;

import org.eclipse.core.runtime.Path;
import org.eclipse.debug.internal.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.debug.internal.ui.sourcelookup.ISourceContainerBrowser;
import org.eclipse.swt.widgets.Shell;

/**
 * The browser for adding an external folder source container.
 * @since 3.0
 */
public class DirectorySourceContainerBrowser implements ISourceContainerBrowser {
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.sourcelookup.ISourceContainerBrowser#createSourceContainers(org.eclipse.swt.widgets.Shell)
	 */
	public ISourceContainer[] createSourceContainers(Shell shell) {
		ISourceContainer[] containers = new ISourceContainer[1];
		DirectorySourceContainerDialog dialog = new DirectorySourceContainerDialog(shell);
		String result = dialog.getResult();
		if(result !=null)
		{	//TODO add boolean to dialog instead of hard coding
			containers[0] = new DirectorySourceContainer(new Path(result), true);			
			return containers;			
		}
		
		return new ISourceContainer[0];
	}
	
}

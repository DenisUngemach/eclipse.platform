/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.data;

import java.net.URL;

/**
 * The input to the parser which can be the URL of a file
 * or a string of XML
 */

public class ParserInput {
	private URL url;
	private String xml;
	
	public ParserInput() {
		url = null;
		xml = null;
	}
	
	public ParserInput(String xml) {
		this.xml = xml;
		this.url = null;
	}
	
	public ParserInput(URL url) {
		this.url = url;
		this.xml = null;
	}
	
	public URL getUrl() {
		return url;
	}
	
	public String getXml() {
		return xml;
	}
	
}

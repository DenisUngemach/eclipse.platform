package org.eclipse.team.internal.ccvs.core.response;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintStream;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.connection.Connection;
import org.eclipse.team.internal.ccvs.core.resources.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.util.Assert;

/**
 * Response to the Clear-static-directory and the Set-static-directory
 * responses of the server.
 * Out of this responses the folder-structure is generated and the
 * information wether the folder a static is set.
 */
class StaticHandler extends ResponseHandler {
	
	public static final String SET_STATIC_RESPONSE = "Set-static-directory";
	public static final String CLEAR_STATIC_RESPONSE = "Clear-static-directory";
	private final boolean setStatic;
		
	/**
	 * Constructor
	 * 
	 * @param setStatic => SetStaticHandler
	           !setStatic => ClearStaticHandler
	 */
	public StaticHandler(boolean setStatic) {
		this.setStatic = setStatic;
	}

	/**
	 * @see IResponseHandler#getName()
	 */
	public String getName() {
		if (setStatic) {
			return SET_STATIC_RESPONSE;
		} else {
			return CLEAR_STATIC_RESPONSE;
		}
	}

	/**
	 * @see IResponseHandler#handle(Connection, PrintStream, ICVSFolder)
	 */
	public void handle(
		Connection connection,
		PrintStream messageOutput,
		ICVSFolder mRoot)
		throws CVSException {
		
		String localDirectory;
		String remoteDirectory;
		
		ICVSFolder mFolder;
		
		// Read the info associated with the Updated response
		localDirectory = connection.readLine();
		remoteDirectory = connection.readLine();
		
		// Cut the last slash form the 
		Assert.isTrue(remoteDirectory.endsWith(SERVER_DELIM));
		remoteDirectory = remoteDirectory.substring(0,remoteDirectory.length() - 
														SERVER_DELIM.length());
		
		createFolder(connection,
					 mRoot,
					 localDirectory,
					 remoteDirectory,
					 "",
					 setStatic,
					 false,
					 true);
	}
}


package org.eclipse.team.internal.ccvs.core.response;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintStream;

import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.connection.Connection;
import org.eclipse.team.internal.ccvs.core.resources.ICVSFile;
import org.eclipse.team.internal.ccvs.core.resources.ICVSFolder;

/**
 * This class responds to the Removed-response of the server<br>
 * It removes the file from both the entries of the parent-folder
 * and from the local filesystem.
 */
class Removed extends ResponseHandler {

	/**
	 * @see IResponseHandler#getName()
	 */
	public String getName() {
		return "Removed";
	}

	/**
	 * @see IResponseHandler#handle(Connection, PrintStream, ICVSFolder)
	 */
	public void handle(
		Connection connection,
		PrintStream messageOutput,
		ICVSFolder mRoot)
		throws CVSException {

		String fileName;
		
		ICVSFolder mParent;
		ICVSFile mFile;
		
		// Read the info associated with the Updated response
		String localDirectory = connection.readLine();
		String repositoryFilename = connection.readLine();

		// Get the local file		
		fileName = repositoryFilename.substring(repositoryFilename.lastIndexOf("/") + 1);
		mParent = mRoot.getFolder(localDirectory);
		mFile = mParent.getFile(fileName);

		Assert.isTrue(mFile.exists() && mFile.isManaged());
		
		// "unmanage" the folder and delete it ...
		mFile.unmanage();
		mFile.delete();

	}

}


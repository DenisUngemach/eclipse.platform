package org.eclipse.team.internal.ccvs.core.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.commands.FileNameMatcher;
import org.eclipse.team.internal.ccvs.core.util.FileUtil;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Implements the ICVSResource interface on top of an 
 * instance of the ICVSResource interface
 * 
 * @see ICVSResource
 */
public abstract class LocalResource implements ICVSResource {

	static final String PLATFORM_NEWLINE = FileUtil.PLATFORM_NEWLINE;
	static final String SERVER_NEWLINE = "\n";
	
	static final byte[] PLATFORM_NEWBYTE = PLATFORM_NEWLINE.getBytes();
	static final byte[] SERVER_NEWBYTE = SERVER_NEWLINE.getBytes();
	
	File ioResource;
	
	public LocalResource(File ioResource) {
		this.ioResource = ioResource;
	}
	
	/**
	 * Get the extention of the path of resource
	 * relative to the path of root
	 * 
	 * @throws CVSException if root is not a root-folder of resource
	 */
	public String getRelativePath(ICVSFolder root) 
		throws CVSException {
		
		LocalResource rootFolder;
		String result;
		
		try {
			rootFolder = (LocalResource)root;
		} catch (ClassCastException e) {
			throw new CVSException(0,0,"two different implementations of ICVSResource used",e);
		}
		
		result = Util.getRelativePath(rootFolder.getPath(),getPath()); 
		return result.replace('\\', '/');	
	}

	/**
	 * Do a DEEP delete.
	 * @see ICVSResource#delete()
	 */
	public void delete() {
		FileUtil.deepDelete(ioResource);
	}

	/**
	 * @see ICVSResource#exists()
	 */
	public boolean exists() {
		return ioResource.exists();
	}

	/**
	 * @see ICVSResource#getParent()
	 */
	public ICVSFolder getParent() {
		return new LocalFolder(ioResource.getParentFile());
	}

	/**
	 * @see ICVSResource#getName()
	 */
	public String getName() {
		return ioResource.getName();
	}

	/**
	 * @see ICVSResource#isIgnored()
	 */
	public boolean isIgnored() throws CVSException {
		FileNameMatcher matcher = FileNameMatcher.getIgnoreMatcherFor(ioResource.getParentFile());
		return (!isManaged() && matcher.match(getName()));
	}

	/**
	 * @see ICVSResource#isManaged()
	 */
	public boolean isManaged() throws CVSException {
		return getSyncInfo() != null;
	}
			
	/**
	 * Two ManagedResources are equal, if there cvsResources are
	 * equal (and that is, if the point to the same file)
	 */
	public boolean equals(Object obj) {
		
		if (!(obj instanceof LocalResource)) {
			return false;
		} else {
			return getPath().equals(((LocalResource) obj).getPath());
		}
	}
			
	/**
	 * Implement the hashcode on the underlying strings, like it
	 * is done in the equals.
	 */
	public int hashCode() {
		return getPath().hashCode();
	}	
	
	/**
	 * @see ICVSResource#getPath()
	 */
	public String getPath() {
		return ioResource.getAbsolutePath();
	}	
	
	/**
	 * Give the pathname back
	 */
	public String toString() {
		return getPath();
	}

	/**
	 * @see ICVSResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}
	
	/*
	 * @see ICVSResource#getSyncInfo()
	 */
	public ResourceSyncInfo getSyncInfo() throws CVSException {
		return Synchronizer.getInstance().getSyncInfo(ioResource);
	}

	/*
	 * @see ICVSResource#setSyncInfo(ResourceSyncInfo)
	 */
	public void setSyncInfo(ResourceSyncInfo info) throws CVSException {
		Synchronizer.getInstance().setSyncInfo(ioResource, info);		
	}
	
	/**
	 * @see ICVSResource#unmanage()
	 */
	public void unmanage() throws CVSException {
		Synchronizer.getInstance().flushSyncInfo(ioResource, IResource.DEPTH_INFINITE);
	}			
}
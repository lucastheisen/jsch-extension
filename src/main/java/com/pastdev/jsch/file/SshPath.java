package com.pastdev.jsch.file;


public interface SshPath extends Iterable<SshPath>, Comparable<SshPath> {
    public SshFileSystem getFileSystem();

    public String getHostname();

    public String getName();

    public int getPort();

    public SshPath getParentPath();

    public String getUsername();

    public SshPath resolve( String other );

    public SshFile toFile();
}

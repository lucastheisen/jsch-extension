package com.pastdev.jsch.file;


import java.net.URI;
import java.net.URISyntaxException;


public interface SshPath extends Iterable<SshPath>, Comparable<SshPath> {
    public SshFileSystem getFileSystem();

    public String getHostname();

    public String getFileName();
    
    public String getName( int index );
    
    public int getNameCount();

    public int getPort();

    public SshPath getParent();

    public SshPath getRoot();

    public String getUsername();

    public SshPath resolve( String other );

    public SshPath toAbsolutePath();

    public URI toUri() throws URISyntaxException;
}

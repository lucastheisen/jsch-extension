package com.pastdev.jsch.file;

import java.net.URI;
import java.net.URISyntaxException;


import com.pastdev.jsch.SessionFactory;


abstract public class AbstractSshPath implements SshPath {
    private SshFileSystem fileSystem;

    protected AbstractSshPath( SshFileSystem fileSystem ) {
        this.fileSystem = fileSystem;
    }

    public SshFileSystem getFileSystem() {
        return fileSystem;
    }

    public String getHostname() {
        return getFileSystem().provider().getSessionFactory().getHostname();
    }

    public String getFileName() {
        return getName( getNameCount() - 1 );
    }

    public int getPort() {
        return getFileSystem().provider().getSessionFactory().getPort();
    }

    public String getUsername() {
        return getFileSystem().provider().getSessionFactory().getUsername();
    }

    public URI toUri() throws URISyntaxException {
        SessionFactory sessionFactory = getFileSystem().provider().getSessionFactory();
        return new URI( "ssh://" + sessionFactory.getUsername() + "@"
                + sessionFactory.getHostname() + ":" + sessionFactory.getPort()
                + toAbsolutePath().toString() );
    }
}

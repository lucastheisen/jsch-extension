package com.pastdev.jsch.file;


import java.net.URI;
import java.net.URISyntaxException;


abstract public class AbstractSshPath implements SshPath {
    private SshFileSystem fileSystem;

    protected AbstractSshPath( SshFileSystem fileSystem ) {
        this.fileSystem = fileSystem;
    }

    public SshFileSystem getFileSystem() {
        return fileSystem;
    }

    abstract public String getHostname();

    public String getFileName() {
        return getName( getNameCount() - 1 );
    }

    abstract public int getPort();

    abstract public String getUsername();

    abstract public URI toUri() throws URISyntaxException;
}

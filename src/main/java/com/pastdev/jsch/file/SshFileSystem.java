package com.pastdev.jsch.file;


import java.io.Closeable;
import java.io.IOException;




abstract public class SshFileSystem implements Closeable {
    private SshFileSystemProvider provider;

    public SshFileSystem( SshFileSystemProvider provider ) {
        this.provider = provider;
    }

    public void close() throws IOException {
        provider.close();
    }
    
    abstract public SshPath getPath( String first, String... more );

    public SshFileSystemProvider provider() {
        return provider;
    }
}

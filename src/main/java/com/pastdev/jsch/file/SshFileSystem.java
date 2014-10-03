package com.pastdev.jsch.file;


import java.io.Closeable;


import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.file.spi.SshFileSystemProvider;


abstract public class SshFileSystem implements Closeable {
    private SshFileSystemProvider provider;

    public SshFileSystem( SshFileSystemProvider provider ) {
        this.provider = provider;
    }

    abstract public CommandRunner getCommandRunner();

    abstract public SshPath getPath( String first, String... more );

    public SshFileSystemProvider provider() {
        return provider;
    }
}

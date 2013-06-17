package com.pastdev.jsch.file;


import com.pastdev.jsch.file.spi.SshFileSystemProvider;


public class UnixSshFileSystem extends SshFileSystem {

    public UnixSshFileSystem( SshFileSystemProvider provider ) {
        super( provider );
    }

    @Override
    public SshPath getPath( String first, String... more ) {
        return new UnixSshPath( this, first, more );
    }
}

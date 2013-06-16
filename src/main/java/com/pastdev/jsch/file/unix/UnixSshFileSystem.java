package com.pastdev.jsch.file.unix;


import com.pastdev.jsch.file.SshFileSystem;
import com.pastdev.jsch.file.SshFileSystemProvider;
import com.pastdev.jsch.file.SshPath;


public class UnixSshFileSystem extends SshFileSystem {

    public UnixSshFileSystem( SshFileSystemProvider provider ) {
        super( provider );
    }

    @Override
    public SshPath getPath( String first, String... more ) {
        //TODO: implement me
        return null;
    }
}

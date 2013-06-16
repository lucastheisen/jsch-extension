package com.pastdev.jsch.file.unix;


import com.pastdev.jsch.file.AbstractSshPath;
import com.pastdev.jsch.file.SshPath;


public class UnixSshPath extends AbstractSshPath {

    UnixSshPath( UnixSshFileSystem unixSshFileSystem, String first, String[] more ) {
        super( unixSshFileSystem, first, more );
    }

    public int compareTo( SshPath o ) {
        if ( ! getFileSystem().provider().equals( o.getFileSystem().provider() ) ) {
            throw new ClassCastException( "cannot compare paths from 2 different filesystems" );
        }
        return toString().compareTo( ((UnixSshPath)o).toString() );
    }

    @Override
    protected String getSeparator() {
        return "/";
    }
}

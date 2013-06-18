package com.pastdev.jsch.file.spi;


import java.io.IOException;
import java.io.OutputStream;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.attribute.PosixFileAttributes;


public class ScpFileOutputStream extends OutputStream {
    private ScpDirectoryOutputStream outputStream;

    ScpFileOutputStream( SshPath path ) throws JSchException, IOException {
        this( path, path.getFileSystem().provider().readAttributes( path, PosixFileAttributes.class ) );
    }
    
    ScpFileOutputStream( SshPath path, PosixFileAttributes attributes ) throws JSchException, IOException {
        this( ScpEntry.newRegularFileEntry( path, attributes.permissions(), attributes.size() ) );
    }

    ScpFileOutputStream( SshPath path, String mode, long size ) throws JSchException, IOException {
        this( ScpEntry.newRegularFileEntry( path, mode, size ) );
    }
    
    ScpFileOutputStream( ScpEntry entry ) throws JSchException, IOException {
        if ( entry.isDirectory() || entry.isEndOfDirectory() ) {
            throw new UnsupportedOperationException();
        }

        this.outputStream = new ScpDirectoryOutputStream( ScpEntry.newDirectoryEntry( entry.path().getParent(), null ) );
        this.outputStream.putNextEntry( entry );
    }

    @Override
    public void close() throws IOException {
        outputStream.closeEntry();
        outputStream.close();
    }

    @Override
    public void write( int b ) throws IOException {
        outputStream.write( b );
    }
}
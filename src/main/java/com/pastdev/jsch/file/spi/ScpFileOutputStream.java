package com.pastdev.jsch.file.spi;


import java.io.IOException;
import java.io.OutputStream;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.attribute.PosixFileAttributes;


public class ScpFileOutputStream extends OutputStream {
    private ScpDirectoryOutputStream outputStream;

    ScpFileOutputStream( SshPath path, PosixFileAttributes attributes ) throws JSchException, IOException {
        if ( attributes.isDirectory() || attributes.isOther() ) {
            throw new UnsupportedOperationException();
        }

        this.outputStream = new ScpDirectoryOutputStream( path.getParentPath() );
        this.outputStream.putNextEntry( path, attributes );
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
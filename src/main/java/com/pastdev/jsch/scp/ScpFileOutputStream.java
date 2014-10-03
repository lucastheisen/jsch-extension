package com.pastdev.jsch.scp;


import java.io.IOException;
import java.io.OutputStream;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.SessionFactory;


public class ScpFileOutputStream extends OutputStream {
    private ScpOutputStream outputStream;

    ScpFileOutputStream( SessionFactory sessionFactory, String directory, ScpEntry scpEntry ) throws JSchException, IOException {
        this.outputStream = new ScpOutputStream( sessionFactory, directory, CopyMode.FILE_ONLY );
        this.outputStream.putNextEntry( scpEntry );
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
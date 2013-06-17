package com.pastdev.jsch.file.spi;


import java.io.IOException;
import java.io.OutputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.attribute.PosixFileAttributes;


/**
 * Based upon information found <a
 * href="https://blogs.oracle.com/janp/entry/how_the_scp_protocol_works"
 * >here</a>.
 * 
 * @author ltheisen
 * 
 */
public class ScpDirectoryOutputStream extends OutputStream {
    private static Logger logger = LoggerFactory.getLogger( ScpDirectoryOutputStream.class );

    private ScpConnection connection;
    private OutputStream outputStream;

    public ScpDirectoryOutputStream( SshPath path ) throws JSchException, IOException {
        logger.debug( "Opening ScpDirectoryOutputStream to {}", path );
        this.connection = new ScpConnection( path, ScpMode.TO );
    }

    @Override
    public void close() throws IOException {
        logger.debug( "Closing ScpDirectoryOutputStream" );
        connection.close();
        outputStream = null;
    }

    public void closeEntry() throws IOException {
        connection.closeEntry();
        outputStream = null;
    }

    public void putEndOfDirectory() throws IOException {
        connection.putEndOfDirectory();
        outputStream = connection.getCurrentOuputStream();
    }

    public void putNextEntry( SshPath path, PosixFileAttributes attributes ) throws IOException {
        connection.putNextEntry( path, attributes );
        outputStream = connection.getCurrentOuputStream();
    }

    @Override
    public void write( int b ) throws IOException {
        if ( outputStream == null ) {
            throw new IllegalStateException( "no current entry, cannot write" );
        }
        outputStream.write( b );
    }
}
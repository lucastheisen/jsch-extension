package com.pastdev.jsch.scp;


import java.io.IOException;
import java.io.OutputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


/**
 * Based upon information found <a
 * href="https://blogs.oracle.com/janp/entry/how_the_scp_protocol_works"
 * >here</a>.
 * 
 * @author ltheisen
 * 
 */
public class ScpOutputStream extends OutputStream {
    private static Logger logger = LoggerFactory.getLogger( ScpOutputStream.class );

    private ScpConnection connection;
    private OutputStream outputStream;

    public ScpOutputStream( Session session, String path, ScpMode scpMode, CopyMode copyMode ) throws JSchException, IOException {
        logger.debug( "Opening ScpOutputStream" );
        this.connection = new ScpConnection( session, path, scpMode, copyMode );
    }
    
    @Override
    public void close() throws IOException {
        logger.debug( "Closing ScpOutputStream" );
        connection.close();
        outputStream = null;
    }
    
    public void closeEntry() throws IOException {
        connection.closeEntry();
        outputStream = null;
    }

    public void putNextEntry( String name ) throws IOException {
        connection.putNextEntry( ScpEntry.newDirectory( name ) );
        outputStream = connection.getCurrentOuputStream();
    }

    public void putNextEntry( String name, long size ) throws IOException {
        connection.putNextEntry( ScpEntry.newFile( name, size ) );
        outputStream = connection.getCurrentOuputStream();
    }

    public void putNextEntry( ScpEntry entry ) throws IOException {
        connection.putNextEntry( entry );
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
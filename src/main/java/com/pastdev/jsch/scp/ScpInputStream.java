package com.pastdev.jsch.scp;


import java.io.IOException;
import java.io.InputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class ScpInputStream extends InputStream {
    private static Logger logger = LoggerFactory.getLogger( ScpInputStream.class );

    private ScpConnection connection;
    private InputStream inputStream;

    public ScpInputStream( Session session, String path, CopyMode copyMode ) throws JSchException, IOException {
        logger.debug( "Opening ScpInputStream" );
        this.connection = new ScpConnection( session, path, ScpMode.FROM, copyMode );
    }

    @Override
    public void close() throws IOException {
        logger.debug( "Closing ScpInputStream" );
        connection.close();
        inputStream = null;
    }

    public void closeEntry() throws IOException {
        connection.closeEntry();
        inputStream = null;
    }

    public ScpEntry getNextEntry() throws IOException {
        ScpEntry entry = connection.getNextEntry();
        inputStream = connection.getCurrentInputStream();
        return entry;
    }

    @Override
    public int read() throws IOException {
        if ( inputStream == null ) {
            throw new IllegalStateException( "no current entry, cannot read" );
        }
        return inputStream.read();
    }
}

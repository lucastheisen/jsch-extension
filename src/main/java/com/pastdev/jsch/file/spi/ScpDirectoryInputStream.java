package com.pastdev.jsch.file.spi;


import java.io.IOException;
import java.io.InputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;


public class ScpDirectoryInputStream extends InputStream {
    private static Logger logger = LoggerFactory.getLogger( ScpDirectoryInputStream.class );

    private ScpConnection connection;
    private InputStream inputStream;

    public ScpDirectoryInputStream( ScpEntry entry ) throws JSchException, IOException {
        if ( !entry.isDirectory() ) {
            throw new UnsupportedOperationException( "can only open ScpDirectoryInputStream on a directory entry" );
        }
        logger.debug( "Opening ScpDirectoryInputStream for {}", entry );
        this.connection = new ScpConnection( entry, ScpMode.FROM );
    }

    @Override
    public void close() throws IOException {
        logger.debug( "Closing ScpDirectoryInputStream" );
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

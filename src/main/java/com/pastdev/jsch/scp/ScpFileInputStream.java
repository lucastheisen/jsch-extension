package com.pastdev.jsch.scp;


import java.io.IOException;
import java.io.InputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class ScpFileInputStream extends InputStream {
    private static Logger logger = LoggerFactory.getLogger( ScpFileInputStream.class );

    private ScpInputStream inputStream;
    private ScpEntry scpEntry;

    ScpFileInputStream( Session session, String path ) throws JSchException, IOException {
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Opening ScpInputStream to {}@{}:{}",
                    new Object[] { session.getUserName(), session.getHost(), path } );
        }
        this.inputStream = new ScpInputStream( session, path, CopyMode.FILE_ONLY );
        this.scpEntry = this.inputStream.getNextEntry();
    }

    public String getMode() {
        return scpEntry.getMode();
    }

    public String getName() {
        return scpEntry.getName();
    }

    public long getSize() {
        return scpEntry.getSize();
    }

    @Override
    public void close() throws IOException {
        logger.debug( "Closing ScpInputStream" );
        inputStream.closeEntry();
        inputStream.close();
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }
}
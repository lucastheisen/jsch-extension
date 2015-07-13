package com.pastdev.jsch.sftp;


import java.io.Closeable;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.SessionManager;


public class SftpRunner implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger( SftpRunner.class );
    private static final String CHANNEL_SFTP = "sftp";

    private final SessionManager sessionManager;

    public SftpRunner( SessionFactory sessionFactory ) {
        this.sessionManager = new SessionManager( sessionFactory );
    }
    
    public void execute( Sftp sftp ) throws JSchException, IOException {
        logger.debug( "executing sftp command on {}", sessionManager );
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp)sessionManager.getSession()
                .openChannel( CHANNEL_SFTP );
            channelSftp.connect();
            sftp.run( channelSftp );
        }
        finally {
            if ( channelSftp != null ) {
                channelSftp.disconnect();
            }
        }
    }

    @Override
    public void close() throws IOException {
        sessionManager.close();
    }
    
    public static interface Sftp {
        public void run( ChannelSftp sftp ) throws JSchException, IOException;
    }
}

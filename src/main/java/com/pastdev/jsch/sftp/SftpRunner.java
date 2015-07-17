package com.pastdev.jsch.sftp;


import java.io.Closeable;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.SessionManager;


/**
 * Provides a convenience wrapper around an <code>sftp</code> channel. This
 * implementation offers a simplified interface that manages the resources
 * needed to issue <code>sftp</code> commands.
 * 
 * @see com.jcraft.jsch.ChannelSftp
 */
public class SftpRunner implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger( SftpRunner.class );
    private static final String CHANNEL_SFTP = "sftp";

    private final SessionManager sessionManager;

    /**
     * Creates a new SftpRunner that will use a {@link SessionManager} that
     * wraps the supplied <code>sessionFactory</code>.
     * 
     * @param sessionFactory
     *            The factory used to create a session manager
     */
    public SftpRunner( SessionFactory sessionFactory ) {
        this.sessionManager = new SessionManager( sessionFactory );
    }

    /**
     * Executes the <code>sftp</code> callback providing it an open
     * {@link ChannelSftp}. Sftp callback implementations should <i>NOT</i>
     * close the channel.
     * 
     * @param sftp A callback
     * @throws JSchException
     *             If ssh execution fails
     * @throws IOException
     *             If unable to read the result data
     */
    public void execute( Sftp sftp ) throws JSchException, IOException {
        logger.debug( "executing sftp command on {}", sessionManager );
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) sessionManager.getSession()
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

    /**
     * Closes the underlying {@link SessionManager}.
     * 
     * @see {@link SessionManager#close()}
     */
    @Override
    public void close() throws IOException {
        sessionManager.close();
    }

    /**
     * A simple callback interface for working with <i>managed</i> sftp channels.
     */
    public static interface Sftp {
        public void run( ChannelSftp sftp ) throws JSchException, IOException;
    }
}

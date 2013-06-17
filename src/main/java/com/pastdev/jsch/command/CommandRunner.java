package com.pastdev.jsch.command;


import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;


public class CommandRunner implements Closeable {
    private static Logger logger = LoggerFactory.getLogger( CommandRunner.class );
    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    private final SessionFactory sessionFactory;
    private Session session;
    private String stdout;
    private String stderr;

    public void close() throws IOException {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }
        session = null;
    }

    public CommandRunner( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
        this.session = null;
    }

    public ChannelExec open( String command ) throws JSchException, IOException {
        logger.debug( "executing {} on {}", command, sessionFactory );
        Session session = getSession();

        ChannelExec channel = (ChannelExec)session.openChannel( "exec" );
        channel.setCommand( command );

        logger.debug( "connecting channel" );
        channel.connect();
        
        return channel;
    }

    public int execute( String command ) throws JSchException, IOException {
        logger.debug( "executing {} on {}", command, sessionFactory );
        Session session = getSession();

        ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
        int exitCode;
        Channel channel = null;
        try {
            channel = session.openChannel( "exec" );
            ((ChannelExec)channel).setCommand( command );

            InputStream inputStream = channel.getInputStream();
            ((ChannelExec)channel).setErrStream( stderrStream );
            channel.setInputStream( null );

            channel.connect();

            // now read stdout cause its the only way to ensure we wait until
            // command completes before closing channel
            IOUtils.copy( inputStream, stdoutStream );

            exitCode = channel.getExitStatus();
        }
        finally {
            if ( channel != null ) {
                channel.disconnect();
            }
        }

        stdout = new String( stdoutStream.toByteArray(), UTF8 );
        stderr = new String( stderrStream.toByteArray(), UTF8 );
        return exitCode;
    }

    private Session getSession() throws JSchException {
        if ( session == null || !session.isConnected() ) {
            logger.debug( "getting new session from factory session" );
            session = sessionFactory.newSession();
            logger.debug( "connecting session" );
            session.connect();
        }
        return session;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public String getStderr() {
        return stderr;
    }

    public String getStdout() {
        return stdout;
    }
}

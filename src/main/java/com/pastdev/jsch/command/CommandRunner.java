package com.pastdev.jsch.command;


import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    public ExecuteResult execute( String command ) throws JSchException, IOException {
        logger.debug( "executing {} on {}", command, sessionFactory );
        Session session = getSession();

        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        int exitCode;
        ChannelExecWrapper channel = null;
        try {
            channel = new ChannelExecWrapper( session, command, null, stdOut, stdErr );
        }
        finally {
            exitCode = channel.close();
        }

        return new ExecuteResult( exitCode, 
                new String( stdOut.toByteArray(), UTF8 ), 
                new String( stdErr.toByteArray(), UTF8 ));
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

    public ChannelExecWrapper open( String command ) throws JSchException, IOException {
        logger.debug( "executing {} on {}", command, sessionFactory );
        return new ChannelExecWrapper( getSession(), command, null, null, null );
    }
    
    public class ExecuteResult {
        private int exitCode;
        private String stderr;
        private String stdout;

        public ExecuteResult( int exitCode, String stdout, String stderr ) {
            this.exitCode = exitCode;
            this.stderr = stderr;
            this.stdout = stdout;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStderr() {
            return stderr;
        }

        public String getStdout() {
            return stdout;
        }
    }

    public class ChannelExecWrapper {
        private ChannelExec channel;
        private String command;
        private OutputStream passedInStdErr;
        private InputStream passedInStdIn;
        private OutputStream passedInStdOut;
        private InputStream stdErr;
        private OutputStream stdIn;
        private InputStream stdOut;

        private ChannelExecWrapper( Session session, String command, InputStream stdIn, OutputStream stdOut, OutputStream stdErr ) throws JSchException, IOException {
            this.command = command;
            this.channel = (ChannelExec) session.openChannel( "exec" );
            if ( stdIn != null ) {
                this.passedInStdIn = stdIn;
                this.channel.setInputStream( stdIn );
            }
            if ( stdOut != null ) {
                this.passedInStdOut = stdOut;
                this.channel.setOutputStream( stdOut );
            }
            if ( stdErr != null ) {
                this.passedInStdErr = stdErr;
                this.channel.setErrStream( stdErr );
            }
            this.channel.setCommand( command );
            this.channel.connect();
        }

        public int close() {
            int exitCode = -2;
            if ( channel != null ) {
                try {
                    // In jsch closing the output stream causes an ssh
                    // message to get sent in another thread. It returns
                    // before the message was actually sent. So now i
                    // wait until the exit status is no longer -1 (active).
                    IOUtils.closeAndLogException( passedInStdIn );
                    IOUtils.closeAndLogException( passedInStdOut );
                    IOUtils.closeAndLogException( passedInStdErr );
                    IOUtils.closeAndLogException( stdIn );
                    IOUtils.closeAndLogException( stdOut );
                    IOUtils.closeAndLogException( stdErr );
                    int i = 0;
                    while ( (exitCode = channel.getExitStatus()) == -1 ) {
                        logger.trace( "waiting for exit {}", i++ );
                        try {
                            Thread.sleep( 100 );
                        }
                        catch ( InterruptedException e ) {}
                    }
                }
                finally {
                    if ( channel.isConnected() ) {
                        channel.disconnect();
                    }
                }
            }
            logger.trace( "`{}` exit {}", command, exitCode );
            return exitCode;
        }

        public InputStream getErrStream() throws IOException {
            if ( stdErr == null ) {
                stdErr = channel.getErrStream();
            }
            return stdErr;
        }

        public InputStream getInputStream() throws IOException {
            if ( stdOut == null ) {
                stdOut = channel.getInputStream();
            }
            return stdOut;
        }

        public OutputStream getOutputStream() throws IOException {
            if ( stdIn == null ) {
                stdIn = channel.getOutputStream();
            }
            return stdIn;
        }
    }
}

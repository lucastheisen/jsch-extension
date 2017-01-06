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
import com.pastdev.jsch.SessionManager;


/**
 * Provides a convenience wrapper around an <code>exec</code> channel. This
 * implementation offers a simplified interface to executing remote commands and
 * retrieving the results of execution.
 * 
 * @see com.jcraft.jsch.ChannelExec
 */
public class CommandRunner implements Closeable {
    private static Logger logger = LoggerFactory.getLogger( CommandRunner.class );
    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    private final SessionManager sessionManager;

    /**
     * Creates a new CommandRunner that will use a {@link SessionManager} that
     * wraps the supplied <code>sessionFactory</code>.
     * 
     * @param sessionFactory The factory used to create a session manager
     */
    public CommandRunner( SessionFactory sessionFactory ) {
        this.sessionManager =  new SessionManager( sessionFactory );
    }

    /**
     * Closes the underlying {@link SessionManager}.
     * 
     * @see SessionManager#close()
     */
    @Override
    public void close() throws IOException {
        sessionManager.close();
    }

    /**
     * Returns a new CommandRunner with the same SessionFactory, but will
     * create a separate session.
     * 
     * @return A duplicate CommandRunner with a different session.
     */
    public CommandRunner duplicate() {
        return new CommandRunner( sessionManager.getSessionFactory() );
    }

    /**
     * Executes <code>command</code> and returns the result. Use this method
     * when the command you are executing requires no input, writes only UTF-8
     * compatible text to STDOUT and/or STDERR, and you are comfortable with
     * buffering up all of that data in memory. Otherwise, use
     * {@link #open(String)}, which allows you to work with the underlying
     * streams.
     * 
     * @param command
     *            The command to execute
     * @return The resulting data
     * 
     * @throws JSchException
     *             If ssh execution fails
     * @throws IOException
     *             If unable to read the result data
     */
    public ExecuteResult execute( String command ) throws JSchException, IOException {
        logger.debug( "executing {} on {}", command, sessionManager );
        Session session = sessionManager.getSession();

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
                new String( stdErr.toByteArray(), UTF8 ) );
    }

    /**
     * Executes <code>command</code> and returns an execution wrapper that
     * provides safe access to and management of the underlying streams of data.
     * 
     * @param command
     *            The command to execute
     * @return An execution wrapper that allows you to process the streams
     * @throws JSchException
     *             If ssh execution fails
     * @throws IOException
     *             If unable to read the result data
     */
    public ChannelExecWrapper open( String command ) throws JSchException, IOException {
        logger.debug( "executing {} on {}", command, sessionManager );
        return new ChannelExecWrapper( sessionManager.getSession(), command, null, null, null );
    }

    /**
     * A simple container for the results of a command execution. Contains
     * <ul>
     * <li>The exit code</li>
     * <li>The text written to STDOUT</li>
     * <li>The text written to STDERR</li>
     * </ul>
     * The text will be UTF-8 decoded byte data written by the command.
     */
    public class ExecuteResult {
        private int exitCode;
        private String stderr;
        private String stdout;

        private ExecuteResult( int exitCode, String stdout, String stderr ) {
            this.exitCode = exitCode;
            this.stderr = stderr;
            this.stdout = stdout;
        }

        /**
         * Returns the exit code of the command execution.
         * 
         * @return The exit code
         */
        public int getExitCode() {
            return exitCode;
        }

        /**
         * Returns the text written to STDERR. This will be a UTF-8 decoding of
         * the actual bytes written to STDERR.
         * 
         * @return The text written to STDERR
         */
        public String getStderr() {
            return stderr;
        }

        /**
         * Returns the text written to STDOUT. This will be a UTF-8 decoding of
         * the actual bytes written to STDOUT.
         * 
         * @return The text written to STDOUT
         */
        public String getStdout() {
            return stdout;
        }
    }

    /**
     * Wraps the execution of a command to handle the opening and closing of all
     * the data streams for you. To use this wrapper, you call
     * <code>getXxxStream()</code> for the streams you want to work with, which
     * will return an opened stream. Use the stream as needed then call
     * {@link ChannelExecWrapper#close() close()} on the ChannelExecWrapper
     * itself, which will return the the exit code from the execution of the
     * command.
     */
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

        /**
         * Safely closes all stream, waits for the underlying connection to
         * close, then returns the exit code from the command execution.
         * 
         * @return The exit code from the command execution
         */
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
                    while ( !channel.isClosed() ) {
                        logger.trace( "waiting for exit {}", i++ );
                        try {
                            Thread.sleep( 100 );
                        }
                        catch ( InterruptedException e ) {}
                    }
                    exitCode = channel.getExitStatus();
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

        /**
         * Returns the STDERR stream for you to read from. No need to close this
         * stream independently, instead, when done with all processing, call
         * {@link #close()};
         * 
         * @return The STDERR stream
         * @throws IOException
         *             If unable to read from the stream
         */
        public InputStream getErrStream() throws IOException {
            if ( stdErr == null ) {
                stdErr = channel.getErrStream();
            }
            return stdErr;
        }

        /**
         * Returns the STDOUT stream for you to read from. No need to close this
         * stream independently, instead, when done with all processing, call
         * {@link #close()};
         * 
         * @return The STDOUT stream
         * @throws IOException
         *             If unable to read from the stream
         */
        public InputStream getInputStream() throws IOException {
            if ( stdOut == null ) {
                stdOut = channel.getInputStream();
            }
            return stdOut;
        }

        /**
         * Returns the STDIN stream for you to write to. No need to close this
         * stream independently, instead, when done with all processing, call
         * {@link #close()};
         * 
         * @return The STDIN stream
         * @throws IOException
         *             If unable to write to the stream
         */
        public OutputStream getOutputStream() throws IOException {
            if ( stdIn == null ) {
                stdIn = channel.getOutputStream();
            }
            return stdIn;
        }
    }
}

package com.pastdev.jsch.scp;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Stack;
import java.util.regex.Pattern;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.JSchIOException;


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
    private static final String DEFAULT_DIRECTORY_MODE = "0750";
    private static final String DEFAULT_FILE_MODE = "0640";
    private static final Pattern MODE_PATTERN = Pattern.compile( "[0-2]?[0-7]{3}" );
    private static final Charset US_ASCII = Charset.forName( "US-ASCII" );

    private Channel channel;
    private CopyMode copyMode;
    private ScpFile currentFile;
    private Stack<ScpDirectory> directoryStack;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Session session;

    public ScpOutputStream( Session session, String path ) throws JSchException, IOException {
        this( session, path, CopyMode.FILE_ONLY );
    }

    public ScpOutputStream( Session session, String path, CopyMode copyMode ) throws JSchException, IOException {
        this.session = session;
        this.copyMode = copyMode;

        if ( session.isConnected() ) {
            throw new IllegalStateException( "session must not be connected" );
        }

        logger.debug( "connecting session" );
        session.connect();

        String command = copyMode.getCommand() + path;
        channel = session.openChannel( "exec" );
        logger.debug( "setting exec command to '{}'", command );
        ((ChannelExec)channel).setCommand( command );

        outputStream = channel.getOutputStream();
        inputStream = channel.getInputStream();

        logger.debug( "connecting channel" );
        channel.connect();
        checkAck();
    }

    /**
     * Throws an JSchIOException if ack was in error. Ack codes are:
     * 
     * <pre>
     *   0 for success,
     *   1 for error,
     *   2 for fatal error
     * </pre>
     * 
     * Also throws, IOException if unable to read from the InputStream. If
     * nothing was thrown, ack was a success.
     */
    private void checkAck() throws IOException {
        logger.trace( "wait for ack" );
        int b = inputStream.read();
        logger.debug( "ack response: '{}'", b );
        if ( b == 0 )
            return;

        if ( b == 1 || b == 2 ) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ( (c = inputStream.read()) != '\n' ) {
                c = inputStream.read();
                sb.append( (char)c );
            }
            if ( b == 1 || b == 2 ) {
                throw new JSchIOException( sb.toString() );
            }
        }
    }

    @Override
    public void close() throws IOException {
        finishFile();
        
        if ( directoryStack != null ) {
            while ( !directoryStack.isEmpty() ) finishDirectory();
        }

        outputStream.close();
        inputStream.close();
        if ( channel != null && channel.isConnected() ) {
            channel.disconnect();
        }
        if ( session.isConnected() ) {
            logger.debug( "disconnecting session" );
            session.disconnect();
        }
        super.close();
    }
    
    public void finishDirectory() throws IOException {
        if ( directoryStack == null || directoryStack.isEmpty() ) {
            throw new IOException( "no directories are active" );
        }

        finishFile();

        writeMessage( directoryStack.pop().toEndMessage() );
    }

    public void finishFile() throws IOException {
        if ( currentFile != null ) {
            if ( !currentFile.complete() ) {
                throw new IOException(
                        "must finish writing previous file before initiating nextFile ("
                                + currentFile.written + "!=" + currentFile.size + ")" );
            }
            writeMessage( (byte)0 );
            currentFile = null;
        }
    }

    public void nextDirectory( String name ) throws IOException {
        nextDirectory( name, DEFAULT_DIRECTORY_MODE );
    }

    public void nextDirectory( String name, String mode ) throws IOException {
        if ( copyMode != CopyMode.RECURSIVE ) {
            throw new IOException( "must be CopyMode.RECURSIVE to allow directories" );
        }
        finishFile();

        if ( directoryStack == null ) {
            directoryStack = new Stack<ScpDirectory>();
        }
        ScpDirectory directory = new ScpDirectory( name, mode );
        directoryStack.push( directory );
        writeMessage( directory.toMessage() );
    }

    public void nextFile( String name, long size ) throws IOException {
        nextFile( name, size, DEFAULT_FILE_MODE );
    }

    public void nextFile( String name, long size, String mode ) throws IOException {
        finishFile();

        currentFile = new ScpFile( name, size, mode );
        writeMessage( currentFile.toMessage() );
    }

    private static String standardizeMode( String mode ) throws IOException {
        if ( !MODE_PATTERN.matcher( mode ).matches() ) {
            throw new IOException( "invalid file mode " + mode );
        }
        if ( mode.length() == 3 ) {
            mode = "0" + mode;
        }
        return mode;
    }

    @Override
    public void write( int output ) throws IOException {
        currentFile.increment();
        outputStream.write( output );
    }

    private void writeMessage( String message ) throws IOException {
        writeMessage( message.getBytes( US_ASCII ) );
    }

    private void writeMessage( byte... message ) throws IOException {
        if ( logger.isDebugEnabled() ) {
            logger.debug( "writing message: '{}'", new String( message, US_ASCII ) );
        }
        outputStream.write( message );
        outputStream.flush();
        checkAck();
    }

    public enum CopyMode {
        FILE_ONLY( "scp -t " ),
        RECURSIVE( "scp -r " );
        
        private String command;
        
        private CopyMode( String command ) {
            this.command = command;
        }
        
        public String getCommand() {
            return command;
        }
    }
   
    private class ScpDirectory {
        private String name;
        private String mode;
        
        ScpDirectory( String name, String mode ) throws IOException {
            this.name = name;
            this.mode = standardizeMode( mode );
        }
        
        String toEndMessage() {
            return "E\n";
        }

        String toMessage() {
            return "D" + mode + " 0 " + name + "\n";
        }
    }

    private class ScpFile {
        private long written;
        private String name;
        private long size;
        private String mode;

        ScpFile( String name, long size, String mode ) throws IOException {
            this.name = name;
            this.size = size;
            this.mode = standardizeMode( mode );
            this.written = 0;
        }

        boolean complete() {
            return written == size;
        }

        void increment() throws IOException {
            if ( complete() ) {
                throw new IOException( "too many bytes written for file " + name );
            }
            written++;
        }

        String toMessage() {
            return "C" + mode + " " + size + " " + name + "\n";
        }
    }
}

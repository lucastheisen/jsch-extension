package com.pastdev.jsch.scp;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Stack;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.JSchIOException;


/**
 * Based on protocol information found <a
 * href="https://blogs.oracle.com/janp/entry/how_the_scp_protocol_works"
 * >here</a>
 * 
 * @author LTHEISEN
 * 
 */
public class ScpConnection {
    private static Logger logger = LoggerFactory.getLogger( ScpConnection.class );
    private static final Charset US_ASCII = Charset.forName( "US-ASCII" );

    private Channel channel;
    private Stack<CurrentEntry> entryStack;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Session session;

    public ScpConnection( Session session, String path, ScpMode scpMode, CopyMode copyMode ) throws JSchException, IOException {
        this.session = session;

        if ( session.isConnected() ) {
            throw new IllegalStateException( "session must not be connected" );
        }

        logger.debug( "connecting session" );
        session.connect();

        String command = getCommand( path, scpMode, copyMode );
        channel = session.openChannel( "exec" );
        logger.debug( "setting exec command to '{}'", command );
        ((ChannelExec) channel).setCommand( command );

        outputStream = channel.getOutputStream();
        inputStream = channel.getInputStream();

        logger.debug( "connecting channel" );
        channel.connect();

        if ( scpMode == ScpMode.FROM ) {
            writeAck();
        }
        else if ( scpMode == ScpMode.TO ) {
            checkAck();
        }

        this.entryStack = new Stack<CurrentEntry>();
    }

    private static String getCommand( String path, ScpMode scpMode, CopyMode copyMode ) {
        StringBuilder command = null;
        switch ( scpMode ) {
            case TO:
                command = new StringBuilder( "scp -tq" );
                break;
            case FROM:
                command = new StringBuilder( "scp -fq" );
        }

        if ( copyMode == CopyMode.RECURSIVE ) {
            command.append( "r" );
        }

        return command.append( " " ).append( path ).toString();
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
    private int checkAck() throws IOException {
        logger.trace( "wait for ack" );
        int b = inputStream.read();
        logger.debug( "ack response: '{}'", b );

        if ( b == 1 || b == 2 ) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ( (c = inputStream.read()) != '\n' ) {
                sb.append( (char) c );
            }
            if ( b == 1 || b == 2 ) {
                throw new JSchIOException( sb.toString() );
            }
        }

        return b;
    }

    public void close() throws IOException {
        IOException toThrow = null;
        try {
            while ( !entryStack.isEmpty() ) {
                entryStack.pop().complete();
            }
        }
        catch ( IOException e ) {
            toThrow = e;
        }

        try {
            if ( outputStream != null ) {
                outputStream.close();
            }
        }
        catch ( IOException e ) {
            logger.error( "failed to close outputStream: {}", e.getMessage() );
            logger.debug( "failed to close outputStream:", e );
        }

        try {
            if ( inputStream != null ) {
                inputStream.close();
            }
        }
        catch ( IOException e ) {
            logger.error( "failed to close inputStream: {}", e.getMessage() );
            logger.debug( "failed to close inputStream:", e );
        }

        if ( channel != null && channel.isConnected() ) {
            channel.disconnect();
        }
        if ( session != null && session.isConnected() ) {
            logger.debug( "disconnecting session" );
            session.disconnect();
        }

        if ( toThrow != null ) {
            throw toThrow;
        }
    }

    public void closeEntry() throws IOException {
        entryStack.pop().complete();
    }

    public InputStream getCurrentInputStream() {
        if ( entryStack.isEmpty() ) {
            return null;
        }
        CurrentEntry currentEntry = entryStack.peek();
        return (currentEntry instanceof InputStream) ? (InputStream) currentEntry : null;
    }

    public OutputStream getCurrentOuputStream() {
        if ( entryStack.isEmpty() ) {
            return null;
        }
        CurrentEntry currentEntry = entryStack.peek();
        return (currentEntry instanceof OutputStream) ? (OutputStream) currentEntry : null;
    }

    public ScpEntry getNextEntry() throws IOException {
        if ( !entryStack.isEmpty() && !entryStack.peek().isDirectoryEntry() ) {
            closeEntry();
        }

        ScpEntry entry = parseMessage();
        if ( entry == null ) return null;
        if ( entry.isEndOfDirectory() ) {
            while ( !entryStack.isEmpty() ) {
                boolean isDirectory = entryStack.peek().isDirectoryEntry();
                closeEntry();
                if ( isDirectory ) {
                    break;
                }
            }
        }
        else if ( entry.isDirectory() ) {
            entryStack.push( new InputDirectoryEntry( entry ) );
        }
        else {
            entryStack.push( new EntryInputStream( entry ) );
        }
        return entry;
    }

    /**
     * Parses SCP protocol messages, for example:
     * 
     * <pre>
     *     File:          C0640 13 test.txt 
     *     Directory:     D0750 0 testdir 
     *     End Directory: E
     * </pre>
     * 
     * @return An ScpEntry for a file (C) or directory (D) or null for end of
     *         directory (E)
     * @throws IOException
     */
    private ScpEntry parseMessage() throws IOException {
        char type = (char) checkAck();

        ScpEntry scpEntry = null;
        if ( type == 'E' ) {
            scpEntry = ScpEntry.newEndOfDirectory();
        }
        else if ( type == 'C' || type == 'D' ) {
            String mode = readMessageSegment();
            String sizeString = readMessageSegment();
            if ( sizeString == null ) return null;
            long size = Long.parseLong( sizeString );
            String name = readMessageSegment();
            if ( name == null ) return null;

            scpEntry = type == 'C'
                    ? ScpEntry.newFile( name, size, mode )
                    : ScpEntry.newDirectory( name, mode );
        }
        else {
            throw new UnsupportedOperationException( "unknown protocol message type " + type );
        }
        logger.debug( "read '{}'", scpEntry );
        return scpEntry;
    }

    public void putNextEntry( String name ) throws IOException {
        putNextEntry( ScpEntry.newDirectory( name ) );
    }

    public void putNextEntry( String name, long size ) throws IOException {
        putNextEntry( ScpEntry.newFile( name, size ) );
    }

    public void putNextEntry( ScpEntry entry ) throws IOException {
        if ( entry.isEndOfDirectory() ) {
            while ( !entryStack.isEmpty() ) {
                boolean isDirectory = entryStack.peek().isDirectoryEntry();
                closeEntry();
                if ( isDirectory ) {
                    break;
                }
            }
            return;
        }
        else if ( !entryStack.isEmpty() ) {
            CurrentEntry currentEntry = entryStack.peek();
            if ( !currentEntry.isDirectoryEntry() ) {
                // auto close previous file entry
                closeEntry();
            }
        }

        if ( entry.isDirectory() ) {
            entryStack.push( new OutputDirectoryEntry( entry ) );
        }
        else {
            entryStack.push( new EntryOutputStream( entry ) );
        }
    }

    private String readMessageSegment() throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        for ( ;; bytesRead++ ) {
            byte b = (byte) inputStream.read();
            if ( b == -1 ) return null; // end of stream
            if ( b == ' ' || b == '\n' ) break;
            buffer[bytesRead] = b;
        }
        return new String( buffer, 0, bytesRead, US_ASCII );
    }

    private void writeAck() throws IOException {
        logger.debug( "writing ack" );
        outputStream.write( (byte) 0 );
        outputStream.flush();
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

    private interface CurrentEntry {
        public void complete() throws IOException;

        public boolean isDirectoryEntry();
    }

    private class InputDirectoryEntry implements CurrentEntry {
        private InputDirectoryEntry( ScpEntry entry ) throws IOException {
            writeAck();
        }

        public void complete() throws IOException {
            writeAck();
        }

        public boolean isDirectoryEntry() {
            return true;
        }
    }

    private class OutputDirectoryEntry implements CurrentEntry {
        private OutputDirectoryEntry( ScpEntry entry ) throws IOException {
            writeMessage( "D" + entry.getMode() + " 0 " + entry.getName() + "\n" );
        }

        public void complete() throws IOException {
            writeMessage( "E\n" );
        }

        public boolean isDirectoryEntry() {
            return true;
        }
    }

    private class EntryInputStream extends InputStream implements CurrentEntry {
        private ScpEntry entry;
        private long ioCount;
        private boolean closed;

        public EntryInputStream( ScpEntry entry ) throws IOException {
            this.entry = entry;
            this.ioCount = 0L;

            writeAck();
            this.closed = false;
        }

        @Override
        public void close() throws IOException {
            if ( !closed ) {
                if ( !isComplete() ) {
                    throw new IOException( "stream not finished ("
                            + ioCount + "!=" + entry.getSize() + ")" );
                }
                writeAck();
                checkAck();
                this.closed = true;
            }
        }

        public void complete() throws IOException {
            close();
        }

        private void increment() throws IOException {
            ioCount++;
        }

        private boolean isComplete() {
            return ioCount == entry.getSize();
        }

        public boolean isDirectoryEntry() {
            return false;
        }

        @Override
        public int read() throws IOException {
            if ( isComplete() ) {
                return -1;
            }
            increment();
            return inputStream.read();
        }
    }

    private class EntryOutputStream extends OutputStream implements CurrentEntry {
        private ScpEntry entry;
        private long ioCount;
        private boolean closed;

        public EntryOutputStream( ScpEntry entry ) throws IOException {
            this.entry = entry;
            this.ioCount = 0L;

            writeMessage( "C" + entry.getMode() + " " + entry.getSize() + " " + entry.getName() + "\n" );
            this.closed = false;
        }

        @Override
        public void close() throws IOException {
            if ( !closed ) {
                if ( !isComplete() ) {
                    throw new IOException( "stream not finished ("
                            + ioCount + "!=" + entry.getSize() + ")" );
                }
                writeMessage( (byte) 0 );
                this.closed = true;
            }
        }

        public void complete() throws IOException {
            close();
        }

        private void increment() throws IOException {
            if ( isComplete() ) {
                throw new IOException( "too many bytes written for file " + entry.getName() );
            }
            ioCount++;
        }

        private boolean isComplete() {
            return ioCount == entry.getSize();
        }

        public boolean isDirectoryEntry() {
            return false;
        }

        @Override
        public void write( int b ) throws IOException {
            increment();
            outputStream.write( b );
        }
    }
}

package com.pastdev.jsch;


import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IOUtils {
    private static Logger logger = LoggerFactory.getLogger( IOUtils.class );

    public static void closeAndIgnoreException( Closeable closeable ) {
        if ( closeable != null ) {
            try {
                closeable.close();
            }
            catch ( IOException e ) {
            }
        }
    }

    public static void closeAndLogException( Closeable closeable ) {
        if ( closeable == null ) {
            logger.debug( "closeable was null" );
        }
        else {
            try {
                closeable.close();
            }
            catch ( IOException e ) {
                if ( logger != null ) {
                    logger.error( "failed to close InputStream: {}", e.getMessage() );
                    logger.debug( "failed to close InputStream:", e );
                }
            }
        }
    }

    public static void copy( InputStream from, OutputStream to ) throws IOException {
        ReadableByteChannel in = Channels.newChannel( from );
        WritableByteChannel out = Channels.newChannel( to );

        final ByteBuffer buffer = ByteBuffer.allocateDirect( 16 * 1024 );
        while ( in.read( buffer ) != -1 ) {
            buffer.flip();
            out.write( buffer );
            buffer.compact();
        }
        buffer.flip();
        while ( buffer.hasRemaining() ) {
            out.write( buffer );
        }
    }

    public static void copyFromString( String from, OutputStream to ) throws IOException {
        copyFromString( from, Charset.defaultCharset(), to );
    }

    public static void copyFromString( String from, Charset fromCharset, OutputStream to ) throws IOException {
        to.write( from.getBytes( fromCharset ) );
    }

    public static String copyToString( InputStream from ) throws IOException {
        return copyToString( from, Charset.defaultCharset() );
    }

    public static String copyToString( InputStream from, Charset toCharset ) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] byteBuffer = new byte[1024];
        int bytesRead = 0;
        while ( (bytesRead = from.read( byteBuffer, 0, 1024 )) >= 0 ) {
            builder.append( new String( byteBuffer, 0, bytesRead, toCharset ) );
        }
        return builder.toString();
    }

    public static void deleteFiles( File... files ) {
        for ( File file : files ) {
            file.delete();
        }
    }

    public static String readFile( File file ) throws IOException {
        return readFile( file, Charset.defaultCharset() );
    }

    public static String readFile( File file, Charset charset ) throws IOException {
        String contents = null;
        InputStream from = null;
        try {
            from = new FileInputStream( file );
            contents = copyToString( from, charset );
        }
        finally {
            closeAndLogException( from );
        }
        return contents;
    }

    public static void writeFile( File file, String contents ) throws IOException {
        writeFile( file, contents, Charset.defaultCharset() );
    }

    public static void writeFile( File file, String contents, Charset charset ) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream( file );
            copyFromString( contents, charset, outputStream );
        }
        finally {
            closeAndLogException( outputStream );
        }
    }
}

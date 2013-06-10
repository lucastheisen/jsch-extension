package com.pastdev.jsch;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;


import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.scp.CopyMode;
import com.pastdev.jsch.scp.ScpEntry;
import com.pastdev.jsch.scp.ScpInputStream;
import com.pastdev.jsch.scp.ScpMode;
import com.pastdev.jsch.scp.ScpOutputStream;


public class ScpOutputStreamTest {
    private static Logger logger = LoggerFactory.getLogger( ScpOutputStreamTest.class );
    private static final Charset UTF8 = Charset.forName( "UTF-8" );
    private static JSch jsch;
    private static Properties properties;

    @BeforeClass
    public static void initializeClass() {
        try {
            properties = new Properties();
            properties.load( ClassLoader.getSystemResourceAsStream( "configuration.properties" ) );
        }
        catch ( IOException e ) {
            logger.warn( "cant find properties file (tests will be skipped): {}", e.getMessage() );
            logger.debug( "cant find properties file:", e );
            properties = null;
            return;
        }

        String knownHosts = properties.getProperty( "ssh.knownHosts" );
        String privateKey = properties.getProperty( "ssh.privateKey" );

        JSch.setLogger( new Slf4jBridge() );
        jsch = new JSch();
        try {
            jsch.setKnownHosts( knownHosts );
            jsch.addIdentity( privateKey );
        }
        catch ( JSchException e ) {
            Assume.assumeNoException( e );
        }
    }

    @Before
    public void beforeTest() {
        Assume.assumeNotNull( properties ); // skip tests if properties not set
    }

    private void createFile( File file, String string ) throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter( file );
            writer.write( string );
        }
        finally {
            if ( writer != null ) {
                writer.close();
            }
        }
    }

    @Test
    public void testInputStream() {
        String rootDir = UUID.randomUUID().toString();
        String scpPath = properties.getProperty( "scp.out.test.scpPath" ) + "/" + rootDir;
        String dir = "tmp2";
        String filesystemPath = properties.getProperty( "scp.out.test.filesystemPath" );
        String username = properties.getProperty( "scp.out.test.username" );
        String hostname = "localhost";
        int port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );

        String expected = "Hello, world!";
        String filename = "test_" + UUID.randomUUID().toString() + ".txt";
        String filename2 = "test_" + UUID.randomUUID().toString() + ".txt";

        File rootFile = new File( filesystemPath, rootDir );
        try {
            rootFile.mkdirs();
            File dirFile = new File( rootFile, dir );
            dirFile.mkdirs();
            createFile( new File( rootFile, filename ), expected );
            createFile( new File( dirFile, filename ), expected );
            createFile( new File( rootFile, filename2 ), expected );
        }
        catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }

        try {
            Session session = jsch.getSession( username, hostname, port );
            ScpInputStream inputStream = null;
            try {
                inputStream = new ScpInputStream( session, scpPath, CopyMode.RECURSIVE );
                List<ScpEntry> entries = new ArrayList<ScpEntry>();
                while ( true ) {
                    ScpEntry entry = inputStream.getNextEntry();
                    if ( entry == null ) break;
                    if ( entry.isFile() ) {
                        String data = readFully( inputStream );
                        logger.debug( "read '{}'", data );
                    }
                    entries.add( entry );
                }
            }
            catch ( IOException e ) {
                logger.error( "failed to write to ScpInputStream: {}", e.getMessage() );
                logger.debug( "failed to write to ScpInputStream: ", e );
                Assert.fail( e.getMessage() );
            }
            finally {
                if ( inputStream != null ) {
                    try {
                        inputStream.close();
                    }
                    catch ( IOException e ) {
                        logger.error( "failed to close ScpInputStream: {}", e.getMessage() );
                        logger.debug( "failed to close ScpInputStream: ", e );
                    }
                }
            }
        }
        catch ( JSchException e ) {
            Assert.fail( e.getMessage() );
        }
        finally {
            rootFile.delete();
        }
    }

    @Test
    public void testOutputStream() {
        String scpPath = properties.getProperty( "scp.out.test.scpPath" );
        String dir = "tmp2";
        String filesystemPath = properties.getProperty( "scp.out.test.filesystemPath" );
        String username = properties.getProperty( "scp.out.test.username" );
        String hostname = "localhost";
        int port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );

        String expected = "Hello, world!";
        String filename = "test_" + UUID.randomUUID().toString() + ".txt";
        String filename2 = "test_" + UUID.randomUUID().toString() + ".txt";

        try {
            Session session = jsch.getSession( username, hostname, port );
            ScpOutputStream outputStream = null;
            try {
                outputStream = new ScpOutputStream( session, scpPath, ScpMode.TO, CopyMode.RECURSIVE );

                outputStream.putNextEntry( filename, expected.length() );
                outputStream.write( expected.getBytes( UTF8 ) );
                outputStream.closeEntry();

                outputStream.putNextEntry( dir );

                outputStream.putNextEntry( filename, expected.length() );
                outputStream.write( expected.getBytes( UTF8 ) );
                outputStream.closeEntry();

                // instead of outputStream.closeEntry() lets try this:
                outputStream.putNextEntry( ScpEntry.newEndOfDirectory() );

                outputStream.putNextEntry( filename2, expected.length() );
                outputStream.write( expected.getBytes( UTF8 ) );
                outputStream.closeEntry();
            }
            catch ( IOException e ) {
                logger.error( "failed to write to ScpOutputStream: {}", e.getMessage() );
                logger.debug( "failed to write to ScpOutputStream: ", e );
                Assert.fail( e.getMessage() );
            }
            finally {
                if ( outputStream != null ) {
                    try {
                        outputStream.close();
                    }
                    catch ( IOException e ) {
                        logger.error( "failed to close ScpOutputStream: {}", e.getMessage() );
                        logger.debug( "failed to close ScpOutputStream: ", e );
                    }
                }
            }

            verifyAndDeleteFile( new File( filesystemPath, filename ), expected );
            verifyAndDeleteFile( new File( filesystemPath, filename2 ), expected );
            File dirFile = new File( filesystemPath, dir );
            verifyAndDeleteFile( new File( dirFile, filename ), expected );
            dirFile.delete();
        }
        catch ( JSchException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private String readFully( InputStream inputStream ) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] byteBuffer = new byte[1024];
        int bytesRead = 0;
        while ( (bytesRead = inputStream.read( byteBuffer, 0, 1024 )) >= 0 ) {
            builder.append( new String( byteBuffer, 0, bytesRead, UTF8 ) );
        }
        return builder.toString();
    }

    public void verifyAndDeleteFile( File file, String expected ) {
        if ( file.exists() ) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream( file );
                Assert.assertEquals( expected, readFully( inputStream ) );
            }
            catch ( FileNotFoundException e ) {
                logger.error( "failed to read from temp file: {}", e.getMessage() );
                logger.debug( "failed to read from temp file: ", e );
                Assert.fail( e.getMessage() );
            }
            catch ( IOException e ) {
                logger.error( "failed to read from temp file: {}", e.getMessage() );
                logger.debug( "failed to read from temp file: ", e );
                Assert.fail( e.getMessage() );
            }
            finally {
                if ( inputStream != null ) {
                    try {
                        inputStream.close();
                    }
                    catch ( IOException e ) {
                        logger.error( "failed to close temp FileInputStream: {}", e.getMessage() );
                        logger.debug( "failed to close temp FileInputStream: ", e );
                    }
                }
            }

            file.delete();
        }
    }
}

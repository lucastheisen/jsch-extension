package com.pastdev.jsch;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
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
import com.pastdev.jsch.scp.ScpOutputStream;
import com.pastdev.jsch.scp.ScpOutputStream.CopyMode;


public class ScpOutputStreamTest {
    private static Logger logger = LoggerFactory.getLogger( ScpOutputStreamTest.class );
    private static final Charset charset = Charset.forName( "UTF-8" );
    private static JSch jsch;
    private static Properties properties;

    static {
    }

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

        try {
            Session session = jsch.getSession( username, hostname, port );
            ScpOutputStream outputStream = null;
            try {
                outputStream = new ScpOutputStream( session, scpPath, CopyMode.RECURSIVE );
                outputStream.nextFile( filename, expected.length() );
                outputStream.write( expected.getBytes( charset ) );
                outputStream.nextDirectory( dir );
                outputStream.nextFile( filename, expected.length() );
                outputStream.write( expected.getBytes( charset ) );
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
            File dirFile = new File( filesystemPath, dir );
            verifyAndDeleteFile( new File( dirFile, filename ), expected );
            dirFile.delete();
        }
        catch ( JSchException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    public void verifyAndDeleteFile( File file, String expected ) {
        if ( file.exists() ) {
            Reader reader = null;
            try {
                reader = new FileReader( file );
                StringBuilder builder = new StringBuilder();
                char[] charBuffer = new char[1024];
                int charsRead = 0;
                while ( (charsRead = reader.read( charBuffer, 0, 1024 )) >= 0 ) {
                    builder.append( charBuffer, 0, charsRead );
                }
                Assert.assertEquals( expected, builder.toString() );
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
                if ( reader != null ) {
                    try {
                        reader.close();
                    }
                    catch ( IOException e ) {
                        logger.error( "failed to close temp FileReader: {}", e.getMessage() );
                        logger.debug( "failed to close temp FileReader: ", e );
                    }
                }
            }

            file.delete();
        }
    }
}

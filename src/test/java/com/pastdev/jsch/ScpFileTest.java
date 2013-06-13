package com.pastdev.jsch;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.File;
import java.io.IOException;
import java.util.UUID;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Session;
import com.pastdev.jsch.scp.ScpFile;
import com.pastdev.jsch.scp.ScpFileInputStream;
import com.pastdev.jsch.scp.ScpFileOutputStream;


public class ScpFileTest extends ScpTestBase {
    private static Logger logger = LoggerFactory.getLogger( ScpFileTest.class );

    private File dir;
    private String expected = "Oh well, lets just use a different string...";
    private File file;
    private String filename;
    private String rootDir;
    private Session session;

    @After
    public void after() {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }

        IOUtils.deleteFiles( file, dir );
    }

    @Before
    public void before() {
        rootDir = UUID.randomUUID().toString();

        dir = new File( filesystemPath, rootDir );
        assertTrue( dir.mkdirs() );
        logger.debug( "{} created dir {}", dir.exists() ? "succesfully" : "failed to", dir );
        filename = UUID.randomUUID().toString() + ".txt";
        file = new File( dir, filename );

        try {
            session = sessionFactory.getSession( username, hostname, port );
        }
        catch ( Exception e ) {
            logger.error( "failed for {}: {}", filename, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
    }

    @Test
    public void testCopyToFile() {
        String fromFilename = "expected.txt";
        File fromFile = new File( dir, fromFilename );
        try {
            IOUtils.writeFile( fromFile, expected, UTF8 );
            ScpFile from = new ScpFile( session, scpPath, rootDir, fromFilename );
            from.copyTo( file );
            String actual = IOUtils.readFile( file, UTF8 );
            assertEquals( expected, actual );
        }
        catch ( Exception e ) {
            logger.error( "failed for {}: {}", filename, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
        IOUtils.deleteFiles( fromFile );
    }

    @Test
    public void testGetInputStream() {
        try {
            ScpFileInputStream scpFileInputStream = null;
            try {
                IOUtils.writeFile( file, expected );
                ScpFile scpFile = new ScpFile( session, scpPath, rootDir, filename );
                scpFileInputStream = scpFile.getInputStream();
                String actual = IOUtils.copyToString( scpFileInputStream, UTF8 );

                assertEquals( expected, actual );
            }
            finally {
                if ( scpFileInputStream != null ) {
                    try {
                        scpFileInputStream.close();
                    }
                    catch ( IOException e ) {
                        logger.error( "failed to close ScpInputStream for {}: {}", filename, e );
                        logger.debug( "failed to close ScpInputStream:", e );
                    }
                }
            }
        }
        catch ( Exception e ) {
            logger.error( "failed for {}: {}", filename, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
    }

    @Test
    public void testGetOutputStream() {
        try {
            ScpFileOutputStream outputStream = null;
            try {
                ScpFile scpFile = new ScpFile( session, scpPath, rootDir, filename );
                outputStream = scpFile.getOutputStream( expected.length() );
                IOUtils.copyFromString( expected, UTF8, outputStream );
            }
            finally {
                if ( outputStream != null ) {
                    try {
                        outputStream.close();
                    }
                    catch ( IOException e ) {
                        logger.error( "failed to close ScpOutputStream for {}: {}", filename, e );
                        logger.debug( "failed to close ScpOutputStream:", e );
                    }
                }
            }

            String actual = IOUtils.readFile( file, UTF8 );
            assertEquals( expected, actual );
        }
        catch ( Exception e ) {
            logger.error( "failed for {}: {}", filename, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
    }
}

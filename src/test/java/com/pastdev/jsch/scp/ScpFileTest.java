package com.pastdev.jsch.scp;


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
import com.pastdev.jsch.IOUtils;


public class ScpFileTest extends ScpTestBase {
    private static Logger logger = LoggerFactory.getLogger( ScpFileTest.class );

    private File dir;
    private String expected = "Oh well, lets just use a different string...";
    private File file;
    private String filename;
    private String rootDir;

    @After
    public void after() {
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
    }

    @Test
    public void testCopyFromFile() {
        String toFilename = "actual.txt";
        File toFile = new File( dir, toFilename );
        try {
            IOUtils.writeFile( file, expected, UTF8 );
            ScpFile to = new ScpFile( sessionFactory, scpPath, rootDir, toFilename );
            to.copyFrom( file );
            String actual = IOUtils.readFile( toFile );
            assertEquals( expected, actual );
        }
        catch ( Exception e ) {
            logger.error( "failed for {}: {}", filename, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( toFile );
        }
    }

    @Test
    public void testCopyToFile() {
        String fromFilename = "expected.txt";
        File fromFile = new File( dir, fromFilename );
        try {
            IOUtils.writeFile( fromFile, expected, UTF8 );
            ScpFile from = new ScpFile( sessionFactory, scpPath, rootDir, fromFilename );
            from.copyTo( file );
            String actual = IOUtils.readFile( file, UTF8 );
            assertEquals( expected, actual );
        }
        catch ( Exception e ) {
            logger.error( "failed for {}: {}", filename, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( fromFile );
        }
    }

    @Test
    public void testCopyToScpFile() {
        String fromFilename = "expected.txt";
        File fromFile = new File( dir, fromFilename );
        Session toSession = null;
        try {
            IOUtils.writeFile( fromFile, expected, UTF8 );
            ScpFile from = new ScpFile( sessionFactory, scpPath, rootDir, fromFilename );
            ScpFile to = new ScpFile( sessionFactory, scpPath, rootDir, filename );
            from.copyTo( to );
            String actual = IOUtils.readFile( file, UTF8 );
            assertEquals( expected, actual );
        }
        catch ( Exception e ) {
            logger.error( "failed for {}: {}", filename, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
        finally {
            if ( toSession != null && toSession.isConnected() ) {
                toSession.disconnect();
            }
            IOUtils.deleteFiles( fromFile );
        }
    }

    @Test
    public void testGetInputStream() {
        try {
            ScpFileInputStream scpFileInputStream = null;
            try {
                IOUtils.writeFile( file, expected );
                ScpFile scpFile = new ScpFile( sessionFactory, scpPath, rootDir, filename );
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
                ScpFile scpFile = new ScpFile( sessionFactory, scpPath, rootDir, filename );
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

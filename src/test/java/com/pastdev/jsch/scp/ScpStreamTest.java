package com.pastdev.jsch.scp;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Session;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.scp.CopyMode;
import com.pastdev.jsch.scp.ScpEntry;
import com.pastdev.jsch.scp.ScpInputStream;
import com.pastdev.jsch.scp.ScpOutputStream;


public class ScpStreamTest extends ScpTestBase {
    private static Logger logger = LoggerFactory.getLogger( ScpStreamTest.class );

    private File dir1;
    private String dir1Name;
    private File dir2;
    private String dir2Name;
    private String expected1 = "This is a test";
    private String expected2 = "This is only a test";
    private String expected3 = "Of the Emergency Broadcast System";
    private File file1;
    private String file1Name;
    private File file2;
    private String file2Name;
    private File file3;
    private String file3Name;
    private Session session;

    @After
    public void after() {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }

        IOUtils.deleteFiles( file3, file2, file1, dir2, dir1 );
    }

    @Before
    public void before() {
        dir1Name = UUID.randomUUID().toString();
        dir2Name = "dir";

        dir1 = new File( filesystemPath, dir1Name );
        dir2 = new File( dir1, dir2Name );
        assertTrue( dir2.mkdirs() );
        logger.debug( "{} created dir {}", dir2.exists() ? "succesfully" : "failed to", dir2 );

        file1Name = "file1.txt";
        file1 = new File( dir1, file1Name );
        file2Name = "file2.txt";
        file2 = new File( dir2, file2Name );
        file3Name = "file3.txt";
        file3 = new File( dir1, file3Name );

        try {
            session = sessionFactory.getSession( username, hostname, port );
        }
        catch ( Exception e ) {
            logger.error( "failed to initialize session for {}@{}: {}",
                    new Object[] { username, hostname, e } );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
    }

    private String joinPath( List<String> dirs, String file ) {
        int fileIndex = dirs.size();
        String[] parts = dirs.toArray( new String[fileIndex + 1] );
        parts[fileIndex] = file;
        return joinPath( parts );
    }

    private String joinPath( String... parts ) {
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < parts.length; i++ ) {
            if ( i > 0 ) {
                builder.append( "/" );
            }
            builder.append( parts[i] );
        }
        return builder.toString();
    }

    @Test
    public void testInputStream() {
        try {
            IOUtils.writeFile( file1, expected1, UTF8 );
            IOUtils.writeFile( file2, expected2, UTF8 );
            IOUtils.writeFile( file3, expected3, UTF8 );
        }
        catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }

        ScpInputStream inputStream = null;
        try {
            inputStream = new ScpInputStream( session, joinPath( scpPath, dir1Name, "*" ), CopyMode.RECURSIVE );
            Map<String, String> fileNameToContents = new HashMap<String, String>();
            List<String> dirs = new ArrayList<String>();
            while ( true ) {
                ScpEntry entry = inputStream.getNextEntry();
                if ( entry == null ) break;
                if ( entry.isDirectory() ) {
                    dirs.add( entry.getName() );
                }
                else if ( entry.isEndOfDirectory() ) {
                    dirs.remove( dirs.size() - 1 );
                }
                if ( entry.isFile() ) {
                    String path = joinPath( dirs, entry.getName() );
                    String data = IOUtils.copyToString( inputStream );
                    fileNameToContents.put( path, data );
                }
            }

            Assert.assertEquals( expected1, fileNameToContents.get( file1Name ) );
            Assert.assertEquals( expected2, fileNameToContents.get( joinPath( dir2Name, file2Name ) ) );
            Assert.assertEquals( expected3, fileNameToContents.get( file3Name ) );
        }
        catch ( Exception e ) {
            logger.error( "failed to write to ScpInputStream: {}", e.getMessage() );
            logger.debug( "failed to write to ScpInputStream: ", e );
            Assert.fail( e.getMessage() );
        }
        finally {
            IOUtils.closeAndLogException( inputStream );
        }
    }

    @Test
    public void testOutputStream() {
        ScpOutputStream outputStream = null;
        try {
            outputStream = new ScpOutputStream( session, joinPath( scpPath, dir1Name ), CopyMode.RECURSIVE );

            outputStream.putNextEntry( file1Name, expected1.length() );
            outputStream.write( expected1.getBytes( UTF8 ) );
            outputStream.closeEntry();

            outputStream.putNextEntry( dir2Name );

            outputStream.putNextEntry( file2Name, expected2.length() );
            outputStream.write( expected2.getBytes( UTF8 ) );
            outputStream.closeEntry();

            // instead of outputStream.closeEntry() lets try this:
            outputStream.putNextEntry( ScpEntry.newEndOfDirectory() );

            outputStream.putNextEntry( file3Name, expected3.length() );
            outputStream.write( expected3.getBytes( UTF8 ) );
            outputStream.closeEntry();
        }
        catch ( Exception e ) {
            logger.error( "failed to write to ScpOutputStream: {}", e.getMessage() );
            logger.debug( "failed to write to ScpOutputStream: ", e );
            Assert.fail( e.getMessage() );
        }
        finally {
            IOUtils.closeAndLogException( outputStream );
        }

        try {
            Assert.assertTrue( file1.exists() );
            Assert.assertEquals( expected1, IOUtils.readFile( file1, UTF8 ) );
            Assert.assertTrue( file2.exists() );
            Assert.assertEquals( expected2, IOUtils.readFile( file2, UTF8 ) );
            Assert.assertTrue( file3.exists() );
            Assert.assertEquals( expected3, IOUtils.readFile( file3, UTF8 ) );
        }
        catch ( Exception e ) {
            logger.error( "failed to read file contents: {}", e.getMessage() );
            logger.debug( "failed to read file contents: ", e );
            Assert.fail( e.getMessage() );
        }
    }
}

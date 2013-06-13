package com.pastdev.jsch;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.scp.CopyMode;
import com.pastdev.jsch.scp.ScpEntry;
import com.pastdev.jsch.scp.ScpInputStream;
import com.pastdev.jsch.scp.ScpOutputStream;


public class ScpStreamTest extends ScpTestBase {
    private static Logger logger = LoggerFactory.getLogger( ScpStreamTest.class );

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
        File file1 = new File( rootFile, filename );
        File file2 = new File( rootFile, filename2 );
        File dirFile = new File( rootFile, dir );
        File file3 = new File( dirFile, filename );
        try {
            dirFile.mkdirs();
            IOUtils.writeFile( file1, expected, UTF8 );
            IOUtils.writeFile( file2, expected, UTF8 );
            IOUtils.writeFile( file3, expected, UTF8 );
        }
        catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }

        try {
            Session session = sessionFactory.getSession( username, hostname, port );
            ScpInputStream inputStream = null;
            try {
                inputStream = new ScpInputStream( session, scpPath + "/*", CopyMode.RECURSIVE );
                Map<String, String> fileNameToContents = new HashMap<String, String>();
                List<String> dirs = new ArrayList<String>();
                while ( true ) {
                    ScpEntry entry = inputStream.getNextEntry();
                    if ( entry == null ) break;
                    if ( entry.isDirectory() ) {
                        dirs.add( entry.getName() );
                    }
                    else if ( entry.isDirectory() ) {
                        dirs.remove( dirs.size() - 1 );
                    }
                    if ( entry.isFile() ) {
                        String path = joinPath( dirs, entry.getName() );
                        String data = IOUtils.copyToString( inputStream );
                        fileNameToContents.put( path, data );
                    }
                }

                Assert.assertEquals( expected, fileNameToContents.get( filename ) );
                Assert.assertEquals( expected, fileNameToContents.get( filename2 ) );
                Assert.assertEquals( expected, fileNameToContents.get( joinPath( Arrays.asList( new String[] { dir } ), filename ) ) );
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
            IOUtils.deleteFiles( file3, dirFile, file2, file1, rootFile );
        }
    }

    @Test
    public void testOutputStream() {
        String rootDir = UUID.randomUUID().toString();
        String scpPath = joinPath( properties.getProperty( "scp.out.test.scpPath" ), rootDir );
        String dir = "tmp2";
        String filesystemPath = properties.getProperty( "scp.out.test.filesystemPath" );
        String username = properties.getProperty( "scp.out.test.username" );
        String hostname = "localhost";
        int port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );

        String expected = "Hello, world!";
        String filename = "test_" + UUID.randomUUID().toString() + ".txt";
        String filename2 = "test_" + UUID.randomUUID().toString() + ".txt";

        File rootFile = new File( filesystemPath, rootDir );
        File file1 = new File( rootFile, filename );
        File file2 = new File( rootFile, filename2 );
        File dirFile = new File( rootFile, dir );
        File file3 = new File( dirFile, filename );
        dirFile.mkdirs();

        try {
            Session session = sessionFactory.getSession( username, hostname, port );
            ScpOutputStream outputStream = null;
            try {
                outputStream = new ScpOutputStream( session, scpPath, CopyMode.RECURSIVE );

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

            Assert.assertTrue( file1.exists() );
            Assert.assertEquals( expected, IOUtils.readFile( file1, UTF8 ) );
            Assert.assertTrue( file2.exists() );
            Assert.assertEquals( expected, IOUtils.readFile( file2, UTF8 ) );
            Assert.assertTrue( file3.exists() );
            Assert.assertEquals( expected, IOUtils.readFile( file3, UTF8 ) );
        }
        catch ( JSchException e ) {
            Assert.fail( e.getMessage() );
        }
        catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file3, dirFile, file2, file1, rootFile );
        }
    }
}

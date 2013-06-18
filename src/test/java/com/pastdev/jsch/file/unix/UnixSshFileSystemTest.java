package com.pastdev.jsch.file.unix;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;


import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.file.DirectoryStream;
import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.UnixSshFileSystem;
import com.pastdev.jsch.file.attribute.PosixFileAttributes;
import com.pastdev.jsch.file.spi.UnixSshFileSystemProvider;
import com.pastdev.jsch.proxy.SshProxyTest;


public class UnixSshFileSystemTest {
    private static Logger logger = LoggerFactory.getLogger( SshProxyTest.class );
    private static final Charset UTF8 = Charset.forName( "UTF-8" );
    private static UnixSshFileSystem unixSshFileSystem;
    private static Properties properties;
    private static String scpPath;
    private static String filesystemPath;
    private static final String expected = "Lets give em something to talk about.";

    @BeforeClass
    public static void initializeClass() {
        InputStream inputStream = null;
        try {
            inputStream = ClassLoader.getSystemResourceAsStream( "configuration.properties" );
            Assume.assumeNotNull( inputStream );
            properties = new Properties();
            properties.load( inputStream );
        }
        catch ( IOException e ) {
            logger.warn( "cant find properties file (tests will be skipped): {}", e.getMessage() );
            logger.debug( "cant find properties file:", e );
            properties = null;
            return;
        }
        finally {
            if ( inputStream != null ) {
                try {
                    inputStream.close();
                }
                catch ( IOException e ) {
                    // really, i dont care...
                }
            }
        }

        String knownHosts = properties.getProperty( "ssh.knownHosts" );
        String privateKey = properties.getProperty( "ssh.privateKey" );
        String username = properties.getProperty( "scp.out.test.username" );
        String hostname = "localhost";
        scpPath = properties.getProperty( "scp.out.test.scpPath" );
        filesystemPath = properties.getProperty( "scp.out.test.filesystemPath" );
        int port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );

        DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory( username, hostname, port );
        try {
            defaultSessionFactory.setKnownHosts( knownHosts );
            defaultSessionFactory.setIdentityFromPrivateKey( privateKey );
        }
        catch ( JSchException e ) {
            Assume.assumeNoException( e );
        }
        unixSshFileSystem = new UnixSshFileSystem(
                new UnixSshFileSystemProvider( defaultSessionFactory ),
                "/home/ltheisen" );
    }

    @Test
    public void newDirectoryStream() {
        String root = UUID.randomUUID().toString();
        String filename1 = "silly1.txt";
        String filename2 = "silly2.txt";
        String filename3 = "silly3.txt";
        String filename4 = "silly4.txt";

        File rootDir = new File( filesystemPath, root );
        File file1 = new File( rootDir, filename1 );
        File file2 = new File( rootDir, filename2 );
        File file3 = new File( rootDir, filename3 );
        File file4 = new File( rootDir, filename4 );
        try {
            rootDir.mkdirs();
            IOUtils.writeFile( file1, expected, UTF8 );
            IOUtils.writeFile( file2, expected, UTF8 );
            IOUtils.writeFile( file3, expected, UTF8 );
            IOUtils.writeFile( file4, expected, UTF8 );
        }
        catch ( IOException e ) {
            logger.error( "could not write files to {}: {}", rootDir, e );
            logger.debug( "could not write to file:", e );
            fail( "could not write files to " + rootDir + ": " + e.getMessage() );
        }

        SshPath rootPath = unixSshFileSystem.getPath( scpPath, root );
        Set<String> expectedEntries = new HashSet<String>();
        expectedEntries.add( rootPath.resolve( filename1 ).toString() );
        expectedEntries.add( rootPath.resolve( filename2 ).toString() );
        expectedEntries.add( rootPath.resolve( filename3 ).toString() );
        expectedEntries.add( rootPath.resolve( filename4 ).toString() );

        try {
            DirectoryStream<SshPath> directoryStream = null;
            try {
                directoryStream = unixSshFileSystem.provider().newDirectoryStream( rootPath );
                for ( SshPath directoryEntry : directoryStream ) {
                    assertTrue( expectedEntries.remove( directoryEntry.toString() ) );
                }
                assertTrue( expectedEntries.isEmpty() );
            }
            finally {
                IOUtils.closeAndLogException( directoryStream );
            }
        }
        catch ( IOException e ) {
            logger.error( "could not obtain directory stream from {}: {}", rootPath, e );
            logger.debug( "could not obtain directory stream:", e );
            fail( "could not obtain directory stream from " + rootPath + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file1, file1, file3, file4, rootDir );
        }
    }

    @Test
    public void testNewInputStream() {
        String root = UUID.randomUUID().toString();
        String filename = "outputstreamtest.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        SshPath path = unixSshFileSystem.getPath( scpPath, root, filename );
        try {
            rootDir.mkdirs();
            
            IOUtils.writeFile( file, expected );
            
            InputStream inputStream = null;
            try {
                inputStream = path.getFileSystem().provider().newInputStream( path );
                assertEquals( expected, IOUtils.copyToString( inputStream ) );
            }
            finally {
                IOUtils.closeAndLogException( inputStream );
            }
        }
        catch ( IOException e ) {
            logger.error( "failed for {}: {}", path, e );
            logger.debug( "failed:", e );
            fail( "failed for " + path + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }
    
    @Test
    public void testNewOutputStream() {
        String root = UUID.randomUUID().toString();
        String filename = "outputstreamtest.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        SshPath path = unixSshFileSystem.getPath( scpPath, root, filename );
        try {
            logger.debug( "making dir {}", rootDir );
            rootDir.mkdirs();

            OutputStream outputStream = null;
            try {
                logger.trace( "getting outputstream" );
                outputStream = path.getFileSystem().provider().newOutputStream( path );
                logger.trace( "writing to outputstream" );
                IOUtils.copyFromString( expected, outputStream );
                logger.trace( "writing complete" );
            }
            finally {
                IOUtils.closeAndLogException( outputStream );
            }

            logger.trace( "checking file contents" );
            assertEquals( expected, IOUtils.readFile( file, UTF8 ) );
            logger.trace( "file contents match" );
        }
        catch ( IOException e ) {
            logger.error( "failed for {}: {}", path, e );
            logger.debug( "failed:", e );
            fail( "failed for " + path + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }

    @Test
    public void testPosixFileAttributes() {
        String root = UUID.randomUUID().toString();
        String filename = "silly.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        try {
            rootDir.mkdirs();
            IOUtils.writeFile( file, expected, UTF8 );
        }
        catch ( IOException e ) {
            logger.error( "could not write to {}: {}", file, e );
            logger.debug( "could not write to file:", e );
            fail( "could not write to " + file + ": " + e.getMessage() );
        }

        SshPath path = unixSshFileSystem.getPath( scpPath, root, filename );
        try {
            Date now = new Date();
            PosixFileAttributes attributes = unixSshFileSystem.provider().readAttributes( path, PosixFileAttributes.class );

            assertTrue( now.after( attributes.creationTime() ) );
            assertTrue( now.after( attributes.lastAccessTime() ) );
            assertTrue( now.after( attributes.lastModifiedTime() ) );
            assertTrue( attributes.isRegularFile() );
            assertFalse( attributes.isDirectory() );
            assertFalse( attributes.isSymbolicLink() );
            assertFalse( attributes.isOther() );
            assertEquals( expected.length(), attributes.size() );
            assertNotNull( attributes.fileKey() );
        }
        catch ( IOException e ) {
            logger.error( "could not read attribues from {}: {}", path, e );
            logger.debug( "could not read attributes:", e );
            fail( "could not read attributes from " + path + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }

    @Test
    public void testReadAttributes() {
        String root = UUID.randomUUID().toString();
        String filename = "silly.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        try {
            rootDir.mkdirs();
            IOUtils.writeFile( file, expected, UTF8 );
        }
        catch ( IOException e ) {
            logger.error( "could not write to {}: {}", file, e );
            logger.debug( "could not write to file:", e );
            fail( "could not write to " + file + ": " + e.getMessage() );
        }

        SshPath path = unixSshFileSystem.getPath( scpPath, root, filename );
        try {
            Date now = new Date();
            Map<String, Object> map = unixSshFileSystem.provider().readAttributes( path, "creationTime,size,fileKey" );

            assertTrue( now.after( (Date) map.get( "creationTime" ) ) );
            assertEquals( Long.valueOf( expected.length() ), (Long) map.get( "size" ) );
            assertNotNull( map.get( "fileKey" ) );
        }
        catch ( IOException e ) {
            logger.error( "could not read attribues from {}: {}", path, e );
            logger.debug( "could not read attributes:", e );
            fail( "could not read attributes from " + path + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }
}

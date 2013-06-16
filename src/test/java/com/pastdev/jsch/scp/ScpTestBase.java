package com.pastdev.jsch.scp;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;


import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.SessionFactory;


public class ScpTestBase {
    private static Logger logger = LoggerFactory.getLogger( ScpTestBase.class );

    protected static final Charset UTF8 = Charset.forName( "UTF-8" );
    protected static SessionFactory sessionFactory;
    protected static Properties properties;
    protected static String scpPath;
    protected static String filesystemPath;

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
        scpPath = properties.getProperty( "scp.out.test.scpPath" );
        filesystemPath = properties.getProperty( "scp.out.test.filesystemPath" );
        String username = properties.getProperty( "scp.out.test.username" );
        String hostname = "localhost";
        int port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );

        DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory( username, hostname, port );
        try {
            defaultSessionFactory.setKnownHosts( knownHosts );
            defaultSessionFactory.setIdentityFromPrivateKey( privateKey );
        }
        catch ( JSchException e ) {
            Assume.assumeNoException( e );
        }
        sessionFactory = defaultSessionFactory;
    }

    @Before
    public void beforeTest() {
        Assume.assumeNotNull( properties ); // skip tests if properties not set
    }
}

package com.pastdev.jsch.proxy;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;


import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;


public class SshProxyTest {
    private static Logger logger = LoggerFactory.getLogger( SshProxyTest.class );
    protected static final Charset UTF8 = Charset.forName( "UTF-8" );
    protected static SessionFactory sessionFactory;
    protected static Properties properties;
    protected static String username;
    protected static String hostname;
    protected static int port;

    private String expected = "there is absolutely no chance this is gonna work!";

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
        username = properties.getProperty( "scp.out.test.username" );
        hostname = "localhost";
        port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );

        sessionFactory = new SessionFactory();
        try {
            sessionFactory.setKnownHosts( knownHosts );
            sessionFactory.setIdentityFromPrivateKey( privateKey );
        }
        catch ( JSchException e ) {
            Assume.assumeNoException( e );
        }
    }

    @Test
    public void testSshProxy() {
        Proxy proxy = null;
        Session session = null;
        Channel channel = null;
        try {
            proxy = new SshProxy( sessionFactory.newSession( username, "localhost", 22 ) );
            session = sessionFactory.newSession( username, hostname, port, proxy );

            session.connect();

            channel = session.openChannel( "exec" );
            ((ChannelExec) channel).setCommand( "echo " + expected );
            InputStream inputStream = channel.getInputStream();
            channel.connect();

            // echo adds \n
            assertEquals( expected + "\n", IOUtils.copyToString( inputStream, UTF8 ) );
        }
        catch ( Exception e ) {
            logger.error( "failed for proxy {}: {}", proxy, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
        finally {
            if ( channel != null && channel.isConnected() ) {
                channel.disconnect();
            }
            if ( session != null && session.isConnected() ) {
                session.disconnect();
            }
        }
    }
}

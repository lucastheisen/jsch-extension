package com.pastdev.jsch;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;


public class ConnectionTest {
    private static Logger logger = LoggerFactory.getLogger( ConnectionTest.class );
    private static String hostname;
    private static int port;
    private static String username;
    private static String correctPassword;
    private static String incorrectPassword;

    @BeforeClass
    public static void initializeClass() {
        InputStream inputStream = null;
        Properties properties = null;
        try {
            inputStream = ClassLoader.getSystemResourceAsStream( "configuration.properties" );
            Assume.assumeNotNull( inputStream );
            properties = new Properties();
            properties.load( inputStream );
        }
        catch ( IOException e ) {
            logger.warn( "cant find properties file (tests will be skipped): {}", e.getMessage() );
            Assume.assumeNoException( e );
        }
        finally {
            IOUtils.closeAndLogException( inputStream );
        }

        username = properties.getProperty( "scp.out.test.username" );
        hostname = properties.getProperty( "scp.out.test.host" );
        correctPassword = properties.getProperty( "scp.out.test.password" );
        port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );

        incorrectPassword = correctPassword + ".";
    }

    private SessionFactory getKeyboardInteractiveAuthenticatingSessionFactory( String password )
            throws IOException {
        final DefaultSessionFactory defaultSessionFactory =
                new DefaultSessionFactory( username, hostname, port );
        defaultSessionFactory.setConfig( "PreferredAuthentications",
                "keyboard-interactive" );
        defaultSessionFactory.setUserInfo( new TestUserInfo( password ) );
        return defaultSessionFactory;
    }

    private SessionFactory getPasswordAuthenticatingSessionFactory( String password )
            throws IOException {
        final DefaultSessionFactory defaultSessionFactory =
                new DefaultSessionFactory( username, hostname, port );
        defaultSessionFactory.setConfig( "PreferredAuthentications", "password" );
        defaultSessionFactory.setPassword( password );
        return defaultSessionFactory;
    }

    private void testKeyboardInteractiveConnectionWithPassword( String password ) throws Exception {
        Session session = null;
        try {
            session = getKeyboardInteractiveAuthenticatingSessionFactory( password )
                    .newSession();
            session.connect();
        }
        finally {
            if ( session != null ) {
                session.disconnect();
            }
        }
    }

    private void testPasswordConnectionWithPassword( String password ) throws Exception {
        Session session = null;
        try {
            session = getPasswordAuthenticatingSessionFactory( password )
                    .newSession();
            session.connect();
        }
        finally {
            if ( session != null ) {
                session.disconnect();
            }
        }
    }

    @Ignore
    @Test
    public void testKeyboardInteractiveConnectionWithCorrectPassword() {
        // Doesnt seem to work with cygwin
        Assume.assumeNotNull( username, correctPassword );
        try {
            testKeyboardInteractiveConnectionWithPassword( correctPassword );
        }
        catch ( Exception e ) {
            fail( e.getMessage() );
        }
    }

    @Ignore
    @Test
    public void testKeyboardInteractiveConnectionWithIncorrectPassword() {
        // Doesnt seem to work with cygwin
        Assume.assumeNotNull( username, incorrectPassword );
        try {
            testKeyboardInteractiveConnectionWithPassword( incorrectPassword );
        }
        catch ( JSchException e ) {
            assertEquals( "Auth fail", e.getMessage() );
        }
        catch ( Exception e ) {
            fail( "Unexpected exception: " + e.getMessage() );
        }
    }

    @Test
    public void testPasswordConnectionWithCorrectPassword() {
        Assume.assumeNotNull( username, correctPassword );
        try {
            testPasswordConnectionWithPassword( correctPassword );
        }
        catch ( Exception e ) {
            fail( e.getMessage() );
        }
    }

    @Test
    public void testPasswordConnectionWithIncorrectPassword() {
        Assume.assumeNotNull( username, incorrectPassword );
        try {
            testPasswordConnectionWithPassword( incorrectPassword );
        }
        catch ( JSchException e ) {
            assertEquals( "Auth fail", e.getMessage() );
        }
        catch ( Exception e ) {
            fail( "Unexpected exception: " + e.getMessage() );
        }
    }

    private static final class TestUserInfo implements UserInfo, UIKeyboardInteractive {
        private String password;

        public TestUserInfo( String password ) {
            this.password = password;
        }

        @Override
        public String[] promptKeyboardInteractive( String destination, String name, String instruction, String[] prompt, boolean[] echo ) {
            logger.debug( "getPassphrase()" );
            return new String[] { password };
        }

        @Override
        public String getPassphrase() {
            logger.debug( "getPassphrase()" );
            return null;
        }

        @Override
        public String getPassword() {
            logger.debug( "getPassword()" );
            return password;
        }

        @Override
        public boolean promptPassword( String message ) {
            logger.debug( "promptPassword({})", message );
            return false;
        }

        @Override
        public boolean promptPassphrase( String message ) {
            logger.debug( "promptPassphrase({})", message );
            return false;
        }

        @Override
        public boolean promptYesNo( String message ) {
            logger.debug( "promptYesNo({})", message );
            return false;
        }

        @Override
        public void showMessage( String message ) {
            logger.debug( "showMessage({})", message );
        }
    }
}

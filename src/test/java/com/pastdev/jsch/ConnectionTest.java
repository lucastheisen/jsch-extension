package com.pastdev.jsch;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.pastdev.jsch.command.CommandRunner;


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
        Assume.assumeNotNull( correctPassword );
        incorrectPassword = correctPassword + ".";
        port = Integer.parseInt( properties.getProperty( "scp.out.test.port" ) );
    }

    private SessionFactory getSessionFactory( String password ) throws IOException {
        final DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory( username, hostname, port );
        defaultSessionFactory.setConfig( "PreferredAuthentications", "keyboard-interactive" ); // to
                                                                                               // exclude
                                                                                               // publickey
                                                                                               // auth
        defaultSessionFactory.setPassword( password );
        return defaultSessionFactory;
    }

    private void testConnectionWithPassword( String password ) throws Exception {
        CommandRunner commandRunner = null;
        try {
            commandRunner = new CommandRunner( getSessionFactory( password ) );
            final String expected = "hello";
            CommandRunner.ExecuteResult result = commandRunner.execute( "echo " + expected );
            assertEquals( 0, result.getExitCode() );
            assertEquals( expected + "\n", result.getStdout() );
            assertEquals( "", result.getStderr() );
        }
        finally {
            IOUtils.closeAndLogException( commandRunner );
        }
    }

    @Test
    public void testConnectionWithCorrectPassword() {
        try {
            testConnectionWithPassword( correctPassword );
        }
        catch ( Exception e ) {
            fail( e.getMessage() );
        }
    }

    @Test
    public void testConnectionWithIncorrectPassword() {
        try {
            testConnectionWithPassword( incorrectPassword );
        }
        catch ( Exception e ) {
            assertEquals( e.getClass(), com.jcraft.jsch.JSchException.class );
            assertEquals( e.getMessage(), "Auth cancel" );
        }
    }
}

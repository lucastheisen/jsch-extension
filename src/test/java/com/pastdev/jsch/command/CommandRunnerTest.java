package com.pastdev.jsch.command;


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


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.command.CommandRunner.ExecuteResult;
import com.pastdev.jsch.proxy.SshProxyTest;


public class CommandRunnerTest {
    private static Logger logger = LoggerFactory.getLogger( SshProxyTest.class );
    protected static final Charset UTF8 = Charset.forName( "UTF-8" );
    protected static SessionFactory sessionFactory;
    protected static Properties properties;

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

    @Test
    public void testCommandRunner() {
        CommandRunner commandRunner = null;
        try {
            commandRunner = new CommandRunner( sessionFactory );

            logger.debug( "run a command" );
            ExecuteResult result = commandRunner.execute( "echo " + expected );
            assertEquals( 0, result.getExitCode() );
            assertEquals( expected + "\n", result.getStdout() );
            assertEquals( "", result.getStderr() );

            // test automatic reconnect...
            commandRunner.close();

            logger.debug( "now try a second command" );
            result = commandRunner.execute( "echo second " + expected );
            assertEquals( 0, result.getExitCode() );
            assertEquals( "second " + expected + "\n", result.getStdout() );
            assertEquals( "", result.getStderr() );

            logger.debug( "and a third command" );
            result = commandRunner.execute( "echo third " + expected );
            assertEquals( 0, result.getExitCode() );
            assertEquals( "third " + expected + "\n", result.getStdout() );
            assertEquals( "", result.getStderr() );

            logger.debug( "wow, they all worked" );
        }
        catch ( Exception e ) {
            logger.error( "failed for command runner {}: {}", commandRunner, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
        finally {
            IOUtils.closeAndLogException( commandRunner );
        }
    }

    @Test
    public void testDetectOs() {
        CommandRunner commandRunner = null;
        try {
            commandRunner = new CommandRunner( sessionFactory );

            logger.debug( "run a command" );
            ExecuteResult result = commandRunner.execute( "ver" );
            if ( result.getExitCode() == 0 ) {
                logger.debug( "likely windows: " + result.getStdout() );
            }
            else {
                result = commandRunner.execute( "uname -a" );
                if ( result.getExitCode() == 0 ) {
                    logger.debug( "likely unix: " + result.getStdout() );
                }
                else {
                    logger.debug( "unknown os: " + result.getStdout() );
                }
            }
            logger.debug( "wow, they all worked" );
        }
        catch ( Exception e ) {
            logger.error( "failed for command runner {}: {}", commandRunner, e );
            logger.debug( "failed:", e );
            fail( e.getMessage() );
        }
        finally {
            IOUtils.closeAndLogException( commandRunner );
        }
    }
}

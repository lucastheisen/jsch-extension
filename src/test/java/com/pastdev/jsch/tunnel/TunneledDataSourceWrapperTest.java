package com.pastdev.jsch.tunnel;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNoException;


import java.io.IOException;
import java.io.InputStream;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.IOUtils;


public class TunneledDataSourceWrapperTest {
    private static Logger logger = LoggerFactory.getLogger( TunneledDataSourceWrapperTest.class );
    private static Properties properties;
    private static DefaultSessionFactory sessionFactory;

    @BeforeClass
    public static void initialize() {
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

        try {
            DataSource dataSource = new DataSource();
            dataSource.setDriverClassName( properties.getProperty( "dataSource.driver" ) );
            dataSource.setPassword( properties.getProperty( "dataSource.password" ) );
            dataSource.setUsername( properties.getProperty( "dataSource.username" ) );
            dataSource.setUrl( properties.getProperty( "dataSource.url" ) );

            dataSource.getConnection().close();

            dataSource.close( true );
            logger.info( "got a connection, so we can assume the configuration is correct..." );
        }
        catch ( Exception e ) {
            logger.warn( "skipping tests as no connection is available: {}", e.getMessage() );
            logger.debug( "skipping tests as no connection is available: {}", e );
            assumeNoException( e );
        }

        String knownHosts = properties.getProperty( "ssh.knownHosts" );
        String privateKey = properties.getProperty( "ssh.privateKey" );

        sessionFactory = new DefaultSessionFactory();
        try {
            sessionFactory.setKnownHosts( knownHosts );
            sessionFactory.setIdentityFromPrivateKey( privateKey );
        }
        catch ( JSchException e ) {
            Assume.assumeNoException( e );
        }
    }

    @Test
    public void testTunneledDataSourceConnection() throws JSchException, IOException {
        assertTrue( true );
        DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setDriverClassName( properties.getProperty( "dataSource.driver" ) );
        dataSource.setPassword( properties.getProperty( "dataSource.password" ) );
        dataSource.setUsername( properties.getProperty( "dataSource.username" ) );
        dataSource.setUrl( properties.getProperty( "dataSource.tunnel.url" ) );
        List<String> pathAndSpecList = new ArrayList<String>();
        pathAndSpecList.add( properties.getProperty( "dataSource.tunnel.pathAndSpec" ) );

        TunneledDataSourceWrapper wrapper = new TunneledDataSourceWrapper(
                new TunnelConnectionManager(
                        sessionFactory,
                        pathAndSpecList ),
                dataSource );

        try {
            DatabaseMetaData metaData = wrapper.getConnection().getMetaData();
            logger.debug( "{} {}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion() );
        }
        catch ( SQLException e ) {
            fail( "failed to obtain metadata: " + e.getMessage() );
        }
        finally {
            IOUtils.closeAndLogException( wrapper );
        }
    }
}

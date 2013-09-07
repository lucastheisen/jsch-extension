package com.pastdev.jsch;


import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;


/**
 * Similar to JSch, but with methods friendlier to dependency injection.
 * 
 * @author Lucas Theisen
 */
public class DefaultSessionFactory implements SessionFactory {
    private static Logger logger = LoggerFactory.getLogger( DefaultSessionFactory.class );
    public static final String PROPERTY_JSCH_DOT_SSH = "jsch.dotSsh";
    public static final String PROPERTY_JSCH_KNOWN_HOSTS_FILE = "jsch.knownHosts.file";
    public static final String PROPERTY_JSCH_PRIVATE_KEY_FILES = "jsch.privateKey.files";

    private Map<String, String> config;
    private File dotSshDir;
    private String hostname;
    private JSch jsch;
    private int port = SSH_PORT;
    private Proxy proxy;
    private String username;

    public DefaultSessionFactory() {
        this( null, null, null );
    }

    public DefaultSessionFactory( String username, String hostname, Integer port ) {
        JSch.setLogger( new Slf4jBridge() );
        jsch = new JSch();

        try {
            setDefaultIdentities();
        }
        catch ( JSchException e ) {
            logger.warn( "Unable to set default identities: ", e );
        }

        try {
            setDefaultKnownHosts();
        }
        catch ( JSchException e ) {
            logger.warn( "Unable to set default known_hosts: ", e );
        }

        if ( username == null ) {
            this.username = System.getProperty( "user.name" ).toLowerCase();
        }
        else {
            this.username = username;
        }

        if ( hostname == null ) {
            this.hostname = "localhost";
        }
        else {
            this.hostname = hostname;
        }

        if ( port == null ) {
            this.port = 22;
        }
        else {
            this.port = port;
        }
    }

    private DefaultSessionFactory( JSch jsch, String username, String hostname, int port, Proxy proxy ) {
        this.jsch = jsch;
        this.username = username;
        this.hostname = hostname;
        this.port = port;
        this.proxy = proxy;
    }

    private void clearIdentityRepository() throws JSchException {
        jsch.setIdentityRepository( null ); // revert to default identity repo
        jsch.removeAllIdentity();
    }

    private File dotSshDir() {
        if ( dotSshDir == null ) {
            String dotSshString = System.getProperty( PROPERTY_JSCH_DOT_SSH );
            if ( dotSshString != null ) {
                dotSshDir = new File( dotSshString );
            }
            else {
                dotSshDir = new File(
                        new File( System.getProperty( "user.home" ) ),
                        ".ssh" );
            }
        }
        return dotSshDir;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public String getUsername() {
        return username;
    }

    public Session newSession() throws JSchException {
        Session session = jsch.getSession( username, hostname, port );
        if ( config != null ) {
            for ( String key : config.keySet() ) {
                session.setConfig( key, config.get( key ) );
            }
        }
        if ( proxy != null ) {
            session.setProxy( proxy );
        }
        return session;
    }

    public SessionFactoryBuilder newSessionFactoryBuilder() throws JSchException {
        return new SessionFactoryBuilder( jsch, username, hostname, port, proxy, config ) {
            @Override
            public SessionFactory build() {
                DefaultSessionFactory sessionFactory = new DefaultSessionFactory( jsch, username, hostname, port, proxy );
                sessionFactory.config = config;
                return sessionFactory;
            }
        };
    }

    public void setConfig( Map<String, String> config ) {
        this.config = config;
    }

    public void setConfig( String key, String value ) {
        if ( config == null ) {
            config = new HashMap<String, String>();
        }
        config.put( key, value );
    }

    private void setDefaultKnownHosts() throws JSchException {
        String knownHosts = System.getProperty( PROPERTY_JSCH_KNOWN_HOSTS_FILE );
        if ( knownHosts != null && !knownHosts.isEmpty() ) {
            setKnownHosts( knownHosts );
        }
        else {
            File knownHostsFile = new File( dotSshDir(), "known_hosts" );
            if ( knownHostsFile.exists() ) {
                setKnownHosts( knownHostsFile.getAbsolutePath() );
            }
        }
    }

    private void setDefaultIdentities() throws JSchException {
        boolean identitiesSet = false;
        try {
            Connector connector = ConnectorFactory.getDefault()
                    .createConnector();
            if ( connector != null ) {
                logger.info( "An AgentProxy Connector was found, check for identities" );
                RemoteIdentityRepository repository = new RemoteIdentityRepository( connector );
                Vector<Identity> identities = repository.getIdentities();
                if ( identities.size() > 0 ) {
                    logger.info( "Using AgentProxy identities: {}", identities );
                    setIdentityRepository( repository );
                    identitiesSet = true;
                }
            }
        }
        catch ( AgentProxyException e ) {
            logger.debug( "Failed to load any keys from AgentProxy:", e );
        }
        if ( !identitiesSet ) {
            List<String> privateKeyFiles = new ArrayList<String>();
            String privateKeyFilesString = System.getProperty( PROPERTY_JSCH_PRIVATE_KEY_FILES );
            if ( privateKeyFilesString != null && !privateKeyFilesString.isEmpty() ) {
                setIdentitiesFromPrivateKeys( Arrays.asList( privateKeyFilesString.split( "," ) ) );
            }

            for ( File file : new File[] {
                    new File( dotSshDir(), "id_rsa" ),
                    new File( dotSshDir(), "id_dsa" ),
                    new File( dotSshDir(), "id_ecdsa" ) } ) {
                if ( file.exists() ) {
                    privateKeyFiles.add( file.getAbsolutePath() );
                }
            }
            logger.info( "Using local identities: {}", privateKeyFiles );
            setIdentitiesFromPrivateKeys( privateKeyFiles );
        }
    }

    public void setHostname( String hostname ) {
        this.hostname = hostname;
    }

    public void setIdentityFromPrivateKey( String privateKey ) throws JSchException {
        clearIdentityRepository();
        jsch.addIdentity( privateKey );
    }

    public void setIdentitiesFromPrivateKeys( List<String> privateKeys ) throws JSchException {
        clearIdentityRepository();
        for ( String privateKey : privateKeys ) {
            jsch.addIdentity( privateKey );
        }
    }

    public void setIdentityRepository( IdentityRepository identityRepository ) {
        jsch.setIdentityRepository( identityRepository );
    }

    public void setKnownHosts( InputStream knownHosts ) throws JSchException {
        jsch.setKnownHosts( knownHosts );
    }

    public void setKnownHosts( String knownHosts ) throws JSchException {
        jsch.setKnownHosts( knownHosts );
    }

    public void setPort( int port ) {
        this.port = port;
    }

    public void setProxy( Proxy proxy ) {
        this.proxy = proxy;
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    @Override
    public String toString() {
        return (proxy == null ? "" : proxy.toString() + " ") +
                "ssh://" + username + "@" + hostname + ":" + port;
    }
}

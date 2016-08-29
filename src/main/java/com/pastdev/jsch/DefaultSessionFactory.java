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
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;


/**
 * The default implementation of {@link com.pastdev.jsch.SessionFactory
 * SessionFactory}. This class provides sane defaults for all
 * <i>conventional</i> configuration including
 * 
 * <p style="margin-left: 20px;">
 * <b>username</b>: System property <code>user.name</code> <br>
 * <b>hostname</b>: localhost <br>
 * <b>port</b>: 22 <br>
 * <b>.ssh directory:</b> System property <code>jsch.dotSsh</code>, or system
 * property <code>user.home</code> concatenated with <code>"/.ssh"</code> <br>
 * <b>known hosts:</b> System property <code>jsch.knownHosts.file</code> or,
 * .ssh directory concatenated with <code>"/known_hosts"</code>. <br>
 * <b>private keys:</b> First checks for an agent proxy using
 * {@link ConnectorFactory#createConnector()}, then system property
 * <code>jsch.privateKey.files</code> split on <code>","</code>, otherwise, .ssh
 * directory concatenated with all 3 of <code>"/id_rsa"</code>,
 * <code>"/id_dsa"</code>, and <code>"/id_ecdsa"</code> if they exist.
 * </p>
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
    private String password;
    private int port = SSH_PORT;
    private Proxy proxy;
    private UserInfo userInfo;
    private String username;

    /**
     * Creates a default DefaultSessionFactory.
     */
    public DefaultSessionFactory() {
        this( null, null, null );
    }

    /**
     * Constructs a DefaultSessionFactory with the supplied properties.
     * 
     * @param username
     *            The username
     * @param hostname
     *            The hostname
     * @param port
     *            The port
     */
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

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Proxy getProxy() {
        return proxy;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public UserInfo getUserInfo() {
        return userInfo;
    }

    @Override
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
        if ( password != null ) {
            session.setPassword( password );
        }
        if ( userInfo != null ) {
            session.setUserInfo( userInfo );
        }
        return session;
    }

    @Override
    public SessionFactoryBuilder newSessionFactoryBuilder() {
        return new SessionFactoryBuilder( jsch, username, hostname, port, proxy, config, userInfo ) {
            @Override
            public SessionFactory build() {
                DefaultSessionFactory sessionFactory = new DefaultSessionFactory( jsch, username, hostname, port, proxy );
                sessionFactory.config = config;
                sessionFactory.password = password;
                sessionFactory.userInfo = userInfo;
                return sessionFactory;
            }
        };
    }

    /**
     * Sets the configuration options for the sessions created by this factory.
     * This method will replace the current SessionFactory <code>config</code>
     * map. If you want to add, rather than replace, see
     * {@link #setConfig(String, String)}. All of these options will be added
     * one at a time using
     * {@link com.jcraft.jsch.Session#setConfig(String, String)
     * Session.setConfig(String, String)}. Details on the supported options can
     * be found in the source for {@link com.jcraft.jsch.Session#applyConfig()}.
     * 
     * @param config
     *            The configuration options
     * 
     * @see com.jcraft.jsch.Session#setConfig(java.util.Hashtable)
     * @see com.jcraft.jsch.Session#applyConfig()
     */
    public void setConfig( Map<String, String> config ) {
        this.config = config;
    }

    /**
     * Adds a single configuration options for the sessions created by this
     * factory. Details on the supported options can be found in the source for
     * {@link com.jcraft.jsch.Session#applyConfig()}.
     * 
     * @param key
     *            The name of the option
     * @param value
     *            The value of the option
     * 
     * @see #setConfig(Map)
     * @see com.jcraft.jsch.Session#setConfig(java.util.Hashtable)
     * @see com.jcraft.jsch.Session#applyConfig()
     */
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
            String privateKeyFilesString = System.getProperty( PROPERTY_JSCH_PRIVATE_KEY_FILES );
            if ( privateKeyFilesString != null && !privateKeyFilesString.isEmpty() ) {
                logger.info( "Using local identities from {}: {}",
                        PROPERTY_JSCH_PRIVATE_KEY_FILES, privateKeyFilesString );
                setIdentitiesFromPrivateKeys( Arrays.asList( privateKeyFilesString.split( "," ) ) );
                identitiesSet = true;
            }
        }
        if ( !identitiesSet ) {
            List<String> privateKeyFiles = new ArrayList<String>();
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

    /**
     * Sets the hostname.
     * 
     * @param hostname
     *            The hostname.
     */
    public void setHostname( String hostname ) {
        this.hostname = hostname;
    }

    /**
     * Configures this factory to use a single identity authenticated by the
     * supplied private key. The private key should be the path to a private key
     * file in OpenSSH format. Clears out the current {@link IdentityRepository}
     * before adding this key.
     * 
     * @param privateKey
     *            Path to a private key file
     * @throws JSchException
     *             If the key is invalid
     */
    public void setIdentityFromPrivateKey( String privateKey ) throws JSchException {
        clearIdentityRepository();
        jsch.addIdentity( privateKey );
    }

    /**
     * Configures this factory to use a list of identities authenticated by the
     * supplied private keys. The private keys should be the paths to a private
     * key files in OpenSSH format. Clears out the current
     * {@link IdentityRepository} before adding these keys.
     * 
     * @param privateKeys
     *            A list of paths to private key files
     * @throws JSchException
     *             If one (or more) of the keys are invalid
     */
    public void setIdentitiesFromPrivateKeys( List<String> privateKeys ) throws JSchException {
        clearIdentityRepository();
        for ( String privateKey : privateKeys ) {
            jsch.addIdentity( privateKey );
        }
    }

    /**
     * Sets the {@link IdentityRepository} for this factory. This will replace
     * any current IdentityRepository, so you should be sure to call this before
     * any of the <code>setIdentit(y|ies)Xxx</code> if you plan on using both.
     * 
     * @param identityRepository
     *            The identity repository
     * 
     * @see JSch#setIdentityRepository(IdentityRepository)
     */
    public void setIdentityRepository( IdentityRepository identityRepository ) {
        jsch.setIdentityRepository( identityRepository );
    }

    /**
     * Sets the known hosts from the stream. Mostly useful if you distribute
     * your known_hosts in the jar for your application rather than allowing
     * users to manage their own known hosts.
     * 
     * @param knownHosts
     *            A stream of known hosts
     * @throws JSchException
     *             If an I/O error occurs
     * 
     * @see JSch#setKnownHosts(InputStream)
     */
    public void setKnownHosts( InputStream knownHosts ) throws JSchException {
        jsch.setKnownHosts( knownHosts );
    }

    /**
     * Sets the known hosts from a file at path <code>knownHosts</code>.
     * 
     * @param knownHosts
     *            The path to a known hosts file
     * @throws JSchException
     *             If an I/O error occurs
     * 
     * @see JSch#setKnownHosts(String)
     */
    public void setKnownHosts( String knownHosts ) throws JSchException {
        jsch.setKnownHosts( knownHosts );
    }

    /**
     * Sets the {@code password} used to authenticate {@code username}. This
     * mode of authentication is not recommended as it would keep the password
     * in memory and if the application dies and writes a heap dump, it would be
     * available. Using {@link Identity} would be better, or even using ssh
     * agent support.
     * 
     * @param password
     *            the password for {@code username}
     */
    public void setPassword( String password ) {
        this.password = password;
    }

    /**
     * Sets the port.
     * 
     * @param port
     *            The port
     */
    public void setPort( int port ) {
        this.port = port;
    }

    /**
     * Sets the proxy through which all connections will be piped.
     * 
     * @param proxy
     *            The proxy
     */
    public void setProxy( Proxy proxy ) {
        this.proxy = proxy;
    }

    /**
     * Sets the {@code UserInfo} for use with {@code keyboard-interactive}
     * authentication.  This may be useful, however, setting the password
     * with {@link #setPassword(String)} is likely sufficient.
     * 
     * @param userInfo
     * 
     * @see <a
     *      href="http://www.jcraft.com/jsch/examples/UserAuthKI.java.html">Keyboard
     *      Interactive Authentication Example</a>
     */
    public void setUserInfo( UserInfo userInfo ) {
        this.userInfo = userInfo;
    }

    /**
     * Sets the username.
     * 
     * @param username
     *            The username
     */
    public void setUsername( String username ) {
        this.username = username;
    }

    @Override
    public String toString() {
        return (proxy == null ? "" : proxy.toString() + " ") +
                "ssh://" + username + "@" + hostname + ":" + port;
    }
}

package com.pastdev.jsch;


import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;


/**
 * Similar to JSch, but with methods friendlier to dependency injection.
 * 
 * @author Lucas Theisen
 */
public class DefaultSessionFactory implements SessionFactory {
    private Map<String, String> config;
    private String hostname;
    private JSch jsch;
    private int port = SSH_PORT;
    private Proxy proxy;
    private String username;

    public DefaultSessionFactory() {
        JSch.setLogger( new Slf4jBridge() );
        jsch = new JSch();
    }

    private DefaultSessionFactory( JSch jsch, String username, String hostname, int port, Proxy proxy ) {
        this.jsch = jsch;
        this.username = username;
        this.hostname = hostname;
        this.port = port;
        this.proxy = proxy;
    }

    public DefaultSessionFactory( String username, String hostname, int port ) {
        this();
        this.username = username;
        this.hostname = hostname;
        this.port = port;
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

    public void setHostname( String hostname ) {
        this.hostname = hostname;
    }

    public void setIdentityFromPrivateKey( String privateKey ) throws JSchException {
        jsch.removeAllIdentity();
        jsch.addIdentity( privateKey );
    }

    public void setIdentitiesFromPrivateKeys( List<String> privateKeys ) throws JSchException {
        jsch.removeAllIdentity();
        for ( String privateKey : privateKeys ) {
            jsch.addIdentity( privateKey );
        }
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
        return (proxy == null ? "" : proxy.toString() + " " ) +
                "ssh://" + username + "@" + hostname + ":" + port;
    }
}

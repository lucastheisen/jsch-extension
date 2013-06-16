package com.pastdev.jsch;


import java.util.List;


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

    public String getUsername() {
        return username;
    }

    public Session newSession() throws JSchException {
        Session session = jsch.getSession( username, hostname, port );
        if ( proxy != null ) {
            session.setProxy( proxy );
        }
        return session;
    }

    public SessionFactoryBuilder newSessionFactoryBuilder() throws JSchException {
        return new SessionFactoryBuilder( jsch, username, hostname, port, proxy ) {
            @Override
            public SessionFactory build() {
                return new DefaultSessionFactory( jsch, username, hostname, port, proxy );
            }
        };
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
        return "ssh://" + username + "@" + hostname + ":" + port;
    }
}

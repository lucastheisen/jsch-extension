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
public class SessionFactory {
    public static final int SSH_PORT = 22;
    
    private JSch jsch;
    private String username;

    public SessionFactory() {
        JSch.setLogger( new Slf4jBridge() );
        jsch = new JSch();
    }
    
    public Session newSession( String host ) throws JSchException {
        return newSession( username, host, SSH_PORT );
    }
    
    public Session newSession( String host, int port ) throws JSchException {
        return newSession( username, host, port );
    }

    public Session newSession( String username, String host, int port ) throws JSchException {
        return jsch.getSession( username, host, port );
    }
    
    public Session newSession( String username, String host, int port, Proxy proxy ) throws JSchException {
        Session session = newSession( username, host, port );
        session.setProxy( proxy );
        return session;
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
    
    public void setUsername( String username ) {
        this.username = username;
    }
}

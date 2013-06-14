package com.pastdev.jsch.tunnel;


import java.io.Closeable;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.Session;


public class TunnelConnection implements Closeable {
    private static Logger logger = LoggerFactory.getLogger( TunnelConnection.class );

    private String destinationHostname;
    private int destinationPort;
    private int localPort;
    private Session session;

    public TunnelConnection( Session session, int localPort, String destinationHostname, int destinationPort ) {
        this.session = session;
        this.localPort = localPort;
        this.destinationHostname = destinationHostname;
        this.destinationPort = destinationPort;

        if ( session.isConnected() ) {
            throw new IllegalStateException( "session must not be connected" );
        }
    }

    public void open() throws JSchException {
        logger.debug( "connecting session" );
        session.connect();

        session.setPortForwardingL( localPort, destinationHostname, destinationPort );
        logger.info( "forwarding {}", this );
    }

    public void close() throws IOException {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }
    }

    @Override
    public String toString() {
        return localPort + ":" + destinationHostname + ":" + destinationPort;
    }
}

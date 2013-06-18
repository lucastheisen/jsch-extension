package com.pastdev.jsch.tunnel;


import java.io.Closeable;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.SessionFactory;


public class TunnelConnection implements Closeable {
    private static Logger logger = LoggerFactory.getLogger( TunnelConnection.class );

    private String destinationHostname;
    private int destinationPort;
    private int localPort;
    private Session session;
    private SessionFactory sessionFactory;

    public TunnelConnection( SessionFactory sessionFactory, int localPort, String destinationHostname, int destinationPort ) {
        this.sessionFactory = sessionFactory;
        this.localPort = localPort;
        this.destinationHostname = destinationHostname;
        this.destinationPort = destinationPort;
    }

    public void open() throws JSchException {
        if ( session != null && session.isConnected() ) {
            return;
        }
        session = sessionFactory.newSession();

        logger.debug( "connecting session" );
        session.connect();

        session.setPortForwardingL( localPort, destinationHostname, destinationPort );
        logger.info( "forwarding {}", this );
    }

    public void close() throws IOException {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }
        session = null;
    }

    @Override
    public String toString() {
        return localPort + ":" + destinationHostname + ":" + destinationPort;
    }
}

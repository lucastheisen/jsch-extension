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

    private Tunnel[] tunnels;
    private Session session;
    private SessionFactory sessionFactory;

    public TunnelConnection( SessionFactory sessionFactory, int localPort, String destinationHostname, int destinationPort ) {
        this.sessionFactory = sessionFactory;
        this.tunnels = new Tunnel[] { new Tunnel( localPort, destinationHostname, destinationPort ) };
    }
    
    public TunnelConnection( SessionFactory sessionFactory, Tunnel... tunnels ) {
        this.sessionFactory = sessionFactory;
        this.tunnels = tunnels;
    }

    public void open() throws JSchException {
        if ( session != null && session.isConnected() ) {
            return;
        }
        session = sessionFactory.newSession();

        logger.debug( "connecting session" );
        session.connect();

        for ( Tunnel tunnel : tunnels ) {
            logger.debug( "adding tunnel {}", tunnel );
            session.setPortForwardingL( 
                    tunnel.getLocalPort(), 
                    tunnel.getDestinationHostname(), 
                    tunnel.getDestinationPort() );
        }
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
        StringBuilder builder = new StringBuilder( sessionFactory.toString() );
        for ( Tunnel tunnel : tunnels ) {
            builder.append( " -L " ).append( tunnel );
        }
        return builder.toString();
    }
}

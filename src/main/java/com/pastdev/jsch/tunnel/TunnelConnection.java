package com.pastdev.jsch.tunnel;


import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;


public class TunnelConnection implements Closeable {
    private static Logger logger = LoggerFactory.getLogger( TunnelConnection.class );

    private Iterable<Tunnel> tunnels;
    private Session session;
    private SessionFactory sessionFactory;

    public TunnelConnection( SessionFactory sessionFactory, int localPort, String destinationHostname, int destinationPort ) {
        this( sessionFactory, new Tunnel( localPort, destinationHostname, destinationPort ) );
    }

    public TunnelConnection( SessionFactory sessionFactory, Tunnel... tunnels ) {
        this( sessionFactory, Arrays.asList( tunnels ) );
    }

    public TunnelConnection( SessionFactory sessionFactory, List<Tunnel> tunnels ) {
        this.sessionFactory = sessionFactory;
        this.tunnels = tunnels;
    }

    public void close() throws IOException {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }
        session = null;
    }
    
    public boolean isOpen() {
        return session != null && session.isConnected();
    }

    public void open() throws JSchException {
        if ( isOpen() ) {
            return;
        }
        session = sessionFactory.newSession();

        logger.debug( "connecting session" );
        session.connect();

        for ( Tunnel tunnel : tunnels ) {
            logger.debug( "adding tunnel {}", tunnel );
            if ( tunnel.getLocalAlias() == null ) {
                session.setPortForwardingL(
                        tunnel.getLocalPort(),
                        tunnel.getDestinationHostname(),
                        tunnel.getDestinationPort() );
            }
            else {
                session.setPortForwardingL(
                        tunnel.getLocalAlias(),
                        tunnel.getLocalPort(),
                        tunnel.getDestinationHostname(),
                        tunnel.getDestinationPort() );
            }
        }
        logger.info( "forwarding {}", this );
    }
    
    public void reopen() throws JSchException {
        IOUtils.closeAndLogException( this );
        open();
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

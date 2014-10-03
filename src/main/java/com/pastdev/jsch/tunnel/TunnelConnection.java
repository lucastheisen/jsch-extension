package com.pastdev.jsch.tunnel;


import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;


public class TunnelConnection implements Closeable {
    private static Logger logger = LoggerFactory.getLogger( TunnelConnection.class );

    private Map<String, Tunnel> tunnelsByDestination;
    private Session session;
    private SessionFactory sessionFactory;
    private Iterable<Tunnel> tunnels;

    public TunnelConnection( SessionFactory sessionFactory, int localPort, String destinationHostname, int destinationPort ) {
        this( sessionFactory, new Tunnel( localPort, destinationHostname, destinationPort ) );
    }

    public TunnelConnection( SessionFactory sessionFactory, Tunnel... tunnels ) {
        this( sessionFactory, Arrays.asList( tunnels ) );
    }

    public TunnelConnection( SessionFactory sessionFactory, List<Tunnel> tunnels ) {
        this.sessionFactory = sessionFactory;
        this.tunnels = tunnels;
        this.tunnelsByDestination = new HashMap<String, Tunnel>();

        for ( Tunnel tunnel : tunnels ) {
            tunnelsByDestination.put( hostnamePortKey( tunnel ), tunnel );
        }
    }

    public void close() throws IOException {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }
        session = null;

        // unnecessary, but seems right to undo what we did
        for ( Tunnel tunnel : tunnels ) {
            tunnel.setAssignedLocalPort( 0 );
        }
    }
    
    public Tunnel getTunnel( String hostname, int port ) {
        return tunnelsByDestination.get( hostnamePortKey( hostname, port ) );
    }
    
    private String hostnamePortKey( Tunnel tunnel ) {
        return hostnamePortKey( tunnel.getDestinationHostname(), 
                tunnel.getDestinationPort() );
    }

    private String hostnamePortKey( String hostname, int port ) {
        return hostname + ":" + port;
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
            int assignedPort = 0;
            if ( tunnel.getLocalAlias() == null ) {
                assignedPort = session.setPortForwardingL(
                        tunnel.getLocalPort(),
                        tunnel.getDestinationHostname(),
                        tunnel.getDestinationPort() );
            }
            else {
                assignedPort = session.setPortForwardingL(
                        tunnel.getLocalAlias(),
                        tunnel.getLocalPort(),
                        tunnel.getDestinationHostname(),
                        tunnel.getDestinationPort() );
            }
            tunnel.setAssignedLocalPort( assignedPort );
            logger.debug( "added tunnel {}", tunnel );
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

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


/**
 * A TunnelConnection represents an ssh connection that opens one or more
 * {@link com.pastdev.jsch.tunnel.Tunnel Tunnel's}.
 */
public class TunnelConnection implements Closeable {
    private static Logger logger = LoggerFactory.getLogger( TunnelConnection.class );

    private Map<String, Tunnel> tunnelsByDestination;
    private Session session;
    private SessionFactory sessionFactory;
    private Iterable<Tunnel> tunnels;

    /**
     * Creates a TunnelConnection using the the <code>sessionFactory</code> to
     * obtain its ssh connection with a single tunnel defined by
     * {@link com.pastdev.jsch.tunnel.Tunnel#Tunnel(int, String, int)
     * Tunnel(localPort, destinationHostname, destinationPort)}.
     * 
     * @param sessionFactory
     *            The sessionFactory
     * @param localPort
     *            The local port to bind to
     * @param destinationHostname
     *            The destination hostname to tunnel to
     * @param destinationPort
     *            The destination port to tunnel to
     */
    public TunnelConnection( SessionFactory sessionFactory, int localPort, String destinationHostname, int destinationPort ) {
        this( sessionFactory, new Tunnel( localPort, destinationHostname, destinationPort ) );
    }

    /**
     * Creates a TunnelConnection using the the <code>sessionFactory</code> to
     * obtain its ssh connection with a list of
     * {@link com.pastdev.jsch.tunnel.Tunnel Tunnel's}.
     * 
     * @param sessionFactory
     *            The sessionFactory
     * @param tunnels
     *            The tunnels
     */
    public TunnelConnection( SessionFactory sessionFactory, Tunnel... tunnels ) {
        this( sessionFactory, Arrays.asList( tunnels ) );
    }

    /**
     * Creates a TunnelConnection using the the <code>sessionFactory</code> to
     * obtain its ssh connection with a list of
     * {@link com.pastdev.jsch.tunnel.Tunnel Tunnel's}.
     * 
     * @param sessionFactory
     *            The sessionFactory
     * @param tunnels
     *            The tunnels
     */
    public TunnelConnection( SessionFactory sessionFactory, List<Tunnel> tunnels ) {
        this.sessionFactory = sessionFactory;
        this.tunnels = tunnels;
        this.tunnelsByDestination = new HashMap<String, Tunnel>();

        for ( Tunnel tunnel : tunnels ) {
            tunnelsByDestination.put( hostnamePortKey( tunnel ), tunnel );
        }
    }

    /**
     * Closes the underlying ssh session causing all tunnels to be closed.
     */
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

    /**
     * Returns the tunnel matching the supplied values, or <code>null</code> if
     * there isn't one that matches.
     * 
     * @param destinationHostname
     *            The tunnels destination hostname
     * @param destinationPort
     *            The tunnels destination port
     * 
     * @return The tunnel matching the supplied values
     */
    public Tunnel getTunnel( String destinationHostname, int destinationPort ) {
        return tunnelsByDestination.get(
                hostnamePortKey( destinationHostname, destinationPort ) );
    }

    private String hostnamePortKey( Tunnel tunnel ) {
        return hostnamePortKey( tunnel.getDestinationHostname(),
                tunnel.getDestinationPort() );
    }

    private String hostnamePortKey( String hostname, int port ) {
        return hostname + ":" + port;
    }

    /**
     * Returns true if the underlying ssh session is open.
     * 
     * @return True if the underlying ssh session is open
     */
    public boolean isOpen() {
        return session != null && session.isConnected();
    }

    /**
     * Opens a session and connects all of the tunnels.
     * 
     * @throws JSchException
     *             If unable to connect
     */
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

    /**
     * Closes, and re-opens the session and all its tunnels. Effectively calls
     * {@link #close()} followed by a call to {@link #open()}.
     * 
     * @throws JSchException
     *             If unable to connect
     */
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

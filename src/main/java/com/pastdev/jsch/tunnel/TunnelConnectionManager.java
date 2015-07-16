package com.pastdev.jsch.tunnel;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.SessionFactory.SessionFactoryBuilder;
import com.pastdev.jsch.proxy.SshProxy;


/**
 * Manages a collection of tunnels. This implementation will:
 * <ul>
 * <li>Ensure a minimum number of ssh connections are made</li>
 * <li>Ensure all connections are open/closed at the same time</li>
 * <li>Provide a convenient syntax for defining tunnels</li>
 * </ul>
 */
public class TunnelConnectionManager implements Closeable {
    private static final Pattern PATTERN_TUNNELS_CFG_COMMENT_LINE = Pattern.compile( "^\\s*(?:#.*)?$" );
    private static Logger logger = LoggerFactory.getLogger( TunnelConnectionManager.class );

    private SessionFactory baseSessionFactory;
    private List<TunnelConnection> tunnelConnections;

    /**
     * Creates a TunnelConnectionManager that will use the
     * <code>baseSessionFactory</code> to obtain its session connections.
     * Because this constructor does not set the tunnel connections for you, you
     * will need to call {@link #setTunnelConnections(Iterable)}.
     * 
     * @param baseSessionFactory
     *            The session factory
     * @throws JSchException
     *             For connection failures
     * 
     * @see {@link #setTunnelConnections(Iterable)}
     */
    public TunnelConnectionManager( SessionFactory baseSessionFactory ) throws JSchException {
        logger.debug( "Creating TunnelConnectionManager" );
        this.baseSessionFactory = baseSessionFactory;
    }

    /**
     * Creates a TunnelConnectionManager that will use the
     * <code>baseSessionFactory</code> to obtain its session connections and
     * provide the tunnels specified.
     * 
     * @param baseSessionFactory
     *            The session factory
     * @param pathAndSpecList
     *            A list of {@link #setTunnelConnections(Iterable) path and
     *            spec} strings
     * @throws JSchException
     *             For connection failures
     * 
     * @see {@link #setTunnelConnections(Iterable)}
     */
    public TunnelConnectionManager( SessionFactory baseSessionFactory, String... pathAndSpecList ) throws JSchException {
        this( baseSessionFactory, Arrays.asList( pathAndSpecList ) );
    }

    /**
     * Creates a TunnelConnectionManager that will use the
     * <code>baseSessionFactory</code> to obtain its session connections and
     * provide the tunnels specified.
     * 
     * @param baseSessionFactory
     *            The session factory
     * @param pathAndSpecList
     *            A list of {@link #setTunnelConnections(Iterable) path and
     *            spec} strings
     * @throws JSchException
     *             For connection failures
     * 
     * @see {@link #setTunnelConnections(Iterable)}
     */
    public TunnelConnectionManager( SessionFactory baseSessionFactory, Iterable<String> pathAndSpecList ) throws JSchException {
        this( baseSessionFactory );
        setTunnelConnections( pathAndSpecList );
    }

    /**
     * Closes all sessions and their associated tunnels.
     * 
     * @see com.pastdev.jsch.tunnel.TunnelConnection#close()
     */
    @Override
    public void close() {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            IOUtils.closeAndLogException( tunnelConnection );
        }
    }

    /**
     * Will re-open any connections that are not still open.
     * 
     * @throws JSchException
     *             For connection failures
     */
    public void ensureOpen() throws JSchException {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            if ( !tunnelConnection.isOpen() ) {
                tunnelConnection.reopen();
            }
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
     * 
     * @see com.pastdev.jsch.tunnel.TunnelConnection#getTunnel(String, int)
     */
    public Tunnel getTunnel( String destinationHostname, int destinationPort ) {
        // might be better to cache, but dont anticipate massive numbers
        // of tunnel connections...
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            Tunnel tunnel = tunnelConnection.getTunnel(
                    destinationHostname, destinationPort );
            if ( tunnel != null ) {
                return tunnel;
            }
        }
        return null;
    }

    /**
     * Opens all the necessary sessions and connects all of the tunnels.
     * 
     * @throws JSchException
     *             For connection failures
     * 
     * @see com.pastdev.jsch.tunnel.TunnelConnection#open()
     */
    public void open() throws JSchException {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            tunnelConnection.open();
        }
    }

    /**
     * Creates a set of tunnel connections based upon the contents of
     * <code>tunnelsConfig</code>. The format of this file is one path and
     * tunnel per line. Comments and empty lines are allowed and are excluded
     * using the pattern <code>^\s*(?:#.*)?$<code>.
     * 
     * @param tunnelsConfig
     * @throws IOException
     *             If unable to read from <code>tunnelsConfig</code>
     * @throws JSchException
     *             For connection failures
     */
    public void setTunnelConnectionsFromFile( File tunnelsConfig ) throws IOException, JSchException {
        List<String> pathAndTunnels = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader( tunnelsConfig ) );
            String line = null;
            while ( (line = reader.readLine()) != null ) {
                if ( PATTERN_TUNNELS_CFG_COMMENT_LINE.matcher( line ).matches() ) {
                    continue;
                }
                pathAndTunnels.add( line );
            }
        }
        finally {
            if ( reader != null ) {
                IOUtils.closeAndLogException( reader );
            }
        }

        setTunnelConnections( pathAndTunnels );
    }

    /**
     * Creates a set of tunnel connections based upon the pathAndTunnels. Each
     * entry of pathAndTunnels must be of the form (in <a
     * href="https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form"
     * >EBNF</a>):
     * 
     * <pre>
     * path and tunnels = path and tunnel, {new line, path and tunnel}
     * path and tunnel = path, "|", tunnel
     * new line = "\n"
     * path = path part, {"->", path part}
     * path part = {user, "@"}, hostname
     * tunnel = {local part}, ":", destination hostname, ":", destination port
     * local part = {local alias, ":"}, local port
     * local alias = hostname
     * local port = port
     * destination hostname = hostname
     * destination port = port
     * user = ? user name ?
     * hostname = ? hostname ?
     * port = ? port ?
     * </pre>
     * 
     * <p>
     * For example:
     * </p>
     * <p>
     * <code>
     * jimhenson@admin.muppets.com->animal@drteethandtheelectricmahem.muppets.com|drteeth:8080:drteeth.muppets.com:80
     * </code>
     * </p>
     * <p>
     * Says open an ssh connection as user <code>jimhenson</code> to host
     * <code>admin.muppets.com</code>. Then, through that connection, open a
     * connection as user <code>animal</code> to host
     * <code>drteethandtheelectricmahem.muppets.com</code>. Then map local port
     * <code>8080</code> on the interface with alias <code>drteeth</code>
     * through the two-hop tunnel to port <code>80</code> on
     * <code>drteeth.muppets.com</code>.
     * </p>
     * 
     * @param pathAndSpecList
     *            A list of path and spec entries
     * 
     * @throws JSchException
     *             For connection failures
     */
    public void setTunnelConnections( Iterable<String> pathAndSpecList ) throws JSchException {
        Map<String, Set<Tunnel>> tunnelMap = new HashMap<String, Set<Tunnel>>();
        for ( String pathAndSpecString : pathAndSpecList ) {
            String[] pathAndSpec = pathAndSpecString.trim().split( "\\|" );
            Set<Tunnel> tunnelList = tunnelMap.get( pathAndSpec[0] );
            if ( tunnelList == null ) {
                tunnelList = new HashSet<Tunnel>();
                tunnelMap.put( pathAndSpec[0], tunnelList );
            }
            tunnelList.add( new Tunnel( pathAndSpec[1] ) );
        }

        tunnelConnections = new ArrayList<TunnelConnection>();
        SessionFactoryCache sessionFactoryCache = new SessionFactoryCache( baseSessionFactory );
        for ( String path : tunnelMap.keySet() ) {
            tunnelConnections.add( new TunnelConnection( sessionFactoryCache.getSessionFactory( path ),
                    new ArrayList<Tunnel>( tunnelMap.get( path ) ) ) );
        }
    }

    /*
     * Used to ensure duplicate paths are not created which will minimize the
     * number of connections needed.
     */
    static class SessionFactoryCache {
        private Map<String, SessionFactory> sessionFactoryByPath;
        private SessionFactory defaultSessionFactory;

        SessionFactoryCache( SessionFactory baseSessionFactory ) {
            this.defaultSessionFactory = baseSessionFactory;
            this.sessionFactoryByPath = new HashMap<String, SessionFactory>();
        }

        public SessionFactory getSessionFactory( String path ) throws JSchException {
            SessionFactory sessionFactory = null;
            String key = null;
            for ( String part : path.split( "\\-\\>" ) ) {
                if ( key == null ) {
                    key = part;
                }
                else {
                    key += "->" + part;
                }
                if ( sessionFactoryByPath.containsKey( key ) ) {
                    sessionFactory = sessionFactoryByPath.get( key );
                    continue;
                }

                SessionFactoryBuilder builder = null;
                if ( sessionFactory == null ) {
                    builder = defaultSessionFactory.newSessionFactoryBuilder();
                }
                else {
                    builder = sessionFactory.newSessionFactoryBuilder()
                            .setProxy( new SshProxy( sessionFactory ) );
                }

                // start with [username@]hostname[:port]
                String[] userAtHost = part.split( "\\@" );
                String hostname = null;
                if ( userAtHost.length == 2 ) {
                    builder.setUsername( userAtHost[0] );
                    hostname = userAtHost[1];
                }
                else {
                    hostname = userAtHost[0];
                }

                // left with hostname[:port]
                String[] hostColonPort = hostname.split( "\\:" );
                builder.setHostname( hostColonPort[0] );
                if ( hostColonPort.length == 2 ) {
                    builder.setPort( Integer.parseInt( hostColonPort[1] ) );
                }

                sessionFactory = builder.build();
            }
            return sessionFactory;
        }
    }
}

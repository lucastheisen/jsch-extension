package com.pastdev.jsch.tunnel;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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


public class TunnelConnectionManager implements Closeable {
    private static final Pattern PATTERN_TUNNELS_CFG_COMMENT_LINE = Pattern.compile( "^\\s*#.*$" );
    private static Logger logger = LoggerFactory.getLogger( TunnelConnectionManager.class );

    private SessionFactory baseSessionFactory;
    private List<TunnelConnection> tunnelConnections;
    private Map<String, Tunnel> routes;

    public TunnelConnectionManager( SessionFactory baseSessionFactory ) throws JSchException, IOException {
        logger.debug( "Creating TunnelConnectionManager" );
        this.baseSessionFactory = baseSessionFactory;
        this.routes = new HashMap<String, Tunnel>();
    }

    public TunnelConnectionManager( SessionFactory baseSessionFactory, Iterable<String> pathAndSpecList ) throws JSchException, IOException {
        this( baseSessionFactory );
        setTunnelConnections( pathAndSpecList );
    }

    @Override
    public void close() {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            IOUtils.closeAndLogException( tunnelConnection );
        }
    }

    public void ensureOpen() throws JSchException {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            if ( !tunnelConnection.isOpen() ) {
                tunnelConnection.reopen();
            }
        }
    }
    
    public Tunnel getTunnelTo( String destinationHostname, int destinationPort ) {
        return routes.get( routeKey( destinationHostname, destinationPort ) );
    }

    public void open() throws JSchException {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            tunnelConnection.open();
        }
    }

    private String routeKey( Tunnel tunnel ) {
        return routeKey( tunnel.getDestinationHostname(), tunnel.getDestinationPort() );
    }

    private String routeKey( String destinationHostname, int destinationPort ) {
        return destinationHostname + ":" + destinationPort;
    }

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
     * entry of pathAndTunnels must look like this:
     * 
     * <code>
     * user@host->tunnelUser@tunnelHost->tunnel2User@tunnel2Host|localAlias:localPort:destinationHostname:destinationPort
     * </code>
     * 
     * @param pathAndSpecList
     * @throws JSchException
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
            
            Tunnel tunnel = new Tunnel( pathAndSpec[1] );
            routes.put( routeKey( tunnel ), tunnel );
            tunnelList.add( tunnel );
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

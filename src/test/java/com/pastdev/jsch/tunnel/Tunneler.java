package com.pastdev.jsch.tunnel;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.proxy.SshProxy;
import com.pastdev.jsch.tunnel.Tunnel;
import com.pastdev.jsch.tunnel.TunnelConnection;


public class Tunneler {
    private static Logger logger = LoggerFactory.getLogger( Tunneler.class );
    private static List<TunnelConnection> tunnelConnections;
    
    static {
        String username = System.getProperty( "user.name" ).toLowerCase();
        String homeDir = System.getProperty( "user.home" );
        DefaultSessionFactory sessionFactory = new DefaultSessionFactory( username, "localhost", 22 );
        try {
            sessionFactory.setKnownHosts( Tunneler.class.getClassLoader().getResourceAsStream( "known_hosts" ) );
            sessionFactory.setIdentityFromPrivateKey( homeDir + "/.ssh/franks_new_key" );
            sessionFactory.setConfig( "PreferredAuthentications", 
                    "publickey,keyboard-interactive,password" );

            SessionFactory asiasPortalMonitor = sessionFactory.newSessionFactoryBuilder()
                    .setHostname( "asias-portal-monitor.mitre.org" )
                    .build();
            SessionFactory adminAsiasAero = sessionFactory.newSessionFactoryBuilder()
                    .setHostname( "admin.asias.aero" )
                    .setUsername( "fsoganda" )
                    .setProxy( new SshProxy( asiasPortalMonitor ) )
                    .build();
            SessionFactory asiasEsN3 = sessionFactory.newSessionFactoryBuilder()
                    .setHostname( "asias-es-n3" )
                    .setUsername( "fsoganda" )
                    .setProxy( new SshProxy( adminAsiasAero ) )
                    .build();

            tunnelConnections = new ArrayList<TunnelConnection>( Arrays.asList( new TunnelConnection[] {
                    new TunnelConnection( asiasEsN3,
                            new Tunnel( 12345, "localhost", 9200 ) )
            } ) );
        }
        catch ( JSchException e ) {
            throw new RuntimeException( e );
        }
    }

    public static void closeTunnels() {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            IOUtils.closeAndLogException( tunnelConnection );
        }
    }

    public static void openTunnels() throws JSchException {
        for ( TunnelConnection tunnelConnection : tunnelConnections ) {
            tunnelConnection.open();
        }
    }

    public static void main( String[] args ) throws JSchException {

        try {
            openTunnels();

            logger.warn( "tunnels open, press enter to quit" );
            new BufferedReader( new InputStreamReader( System.in ) ).readLine();
        }
        catch ( Exception e ) {
            logger.error( "failed:", e );
        }
        finally {
            logger.debug( "closing tunnels now" );
            closeTunnels();
        }
    }
}

package com.pastdev.jsch.tunnel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;






import java.io.IOException;
import java.util.ArrayList;
import java.util.List;










import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;










import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;


public class TunnelConnectionManagerTest {
    private static Logger logger = LoggerFactory.getLogger( TunnelConnectionManagerTest.class );
    
    @Test
    public void testGetTunnelTo() {
        TunnelConnectionManager manager = null;
        try {
            manager = new TunnelConnectionManager( 
                    new DefaultSessionFactory() );
        }
        catch ( JSchException | IOException e ) {
            logger.debug( "unable to create TunnelConnectionManager: ", e );
            fail( "unable to create TunnelConnectionManager: " + e.getMessage() );
        }
        assertNotNull( manager );

        List<String> pathAndSpecList = new ArrayList<String>();
        pathAndSpecList.add( "joe@crabshack|crab:10:imitationcrab:20" );
        pathAndSpecList.add( "bob@redlobster|lobster:15:tail:20" );
        try {
            manager.setTunnelConnections( pathAndSpecList );
        }
        catch ( JSchException e ) {
            logger.debug( "unable to set pathAndSpecList: ", e );
            fail( "unable to setPathAndSpecList: " + e.getMessage() );
        }
        
        Tunnel tunnel = manager.getTunnelTo( "imitationcrab", 20 );
        assertEquals( "crab", tunnel.getLocalAlias() );
        assertEquals( 10, tunnel.getLocalPort() );
        tunnel = manager.getTunnelTo( "tail", 20 );
        assertEquals( "lobster", tunnel.getLocalAlias() );
        assertEquals( 15, tunnel.getLocalPort() );
    }
}
package com.pastdev.jsch;


import static org.junit.Assert.assertEquals;


import java.net.URI;
import java.net.URISyntaxException;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UriTest {
    public static Logger logger = LoggerFactory.getLogger( UriTest.class );

    @Test
    public void testUri() throws URISyntaxException {
        URI uri1 = new URI( "ssh.unix://ltheisen@localhost:22/root/path/" );
        logger.debug( "uri1 is '{}'", uri1 );
        URI uri2 = uri1.resolve( "relative/part" );
        logger.debug( "uri2 is '{}'", uri2 );
        URI uri3 = uri2.resolve( "/new/root/" );
        logger.debug( "uri3 is '{}'", uri3 );
        
        assertEquals( "ltheisen", uri1.getUserInfo() );
        assertEquals( "localhost", uri1.getHost() );
        assertEquals( 22, uri1.getPort() );
    }
}

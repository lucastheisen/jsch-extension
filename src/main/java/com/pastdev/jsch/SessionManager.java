package com.pastdev.jsch;


import java.io.Closeable;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.pastdev.jsch.SessionFactory;


public class SessionManager implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger( SessionManager.class );

    private final SessionFactory sessionFactory;
    private Session session;
    
    public SessionManager( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void close() throws IOException {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }
        session = null;
    }

    public Session getSession() throws JSchException {
        if ( session == null || !session.isConnected() ) {
            logger.debug( "getting new session from factory session" );
            session = sessionFactory.newSession();
            logger.debug( "connecting session" );
            session.connect();
        }
        return session;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
    @Override
    public String toString() {
        return sessionFactory.toString();
    }
}

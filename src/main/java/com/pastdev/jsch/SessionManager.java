package com.pastdev.jsch;


import java.io.Closeable;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


/**
 * Provides a convenience wrapper to sessions that maintains the session
 * connection for you. Every time you obtain your session through a call to
 * {@link #getSession()} the current session will have its connection verified,
 * and will reconnect if necessary.
 */
public class SessionManager implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger( SessionManager.class );

    private final SessionFactory sessionFactory;
    private Session session;

    /**
     * Creates a SessionManager for the supplied <code>sessionFactory</code>.
     * 
     * @param sessionFactory
     *            The session factory
     */
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

    /**
     * Returns a connected session.
     * 
     * @return A connected session
     * 
     * @throws JSchException
     *             If unable to connect the session
     */
    public Session getSession() throws JSchException {
        if ( session == null || !session.isConnected() ) {
            logger.debug( "getting new session from factory session" );
            session = sessionFactory.newSession();
            logger.debug( "connecting session" );
            session.connect();
        }
        return session;
    }

    /**
     * Returns the session factory used by this manager.
     * 
     * @return The session factory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public String toString() {
        return sessionFactory.toString();
    }
}

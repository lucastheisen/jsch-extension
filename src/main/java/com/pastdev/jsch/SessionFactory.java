package com.pastdev.jsch;


import java.util.Map;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;


/**
 * An interface for creating {@link Session} objects from a common
 * configuration. Also supports creation of other SessionFactory instances that
 * are initialized from the same configuration and can be modified as necessary.
 */
public interface SessionFactory {
    public static final int SSH_PORT = 22;

    /**
     * Returns the hostname that sessions built by this factory will connect to.
     * 
     * @return The hostname
     */
    public String getHostname();

    /**
     * Returns the port that sessions built by this factory will connect to.
     * 
     * @return The port
     */
    public int getPort();

    /**
     * Returns the proxy that sessions built by this factory will connect
     * through, if any. If none was configured, <code>null</code> will be
     * returned.
     * 
     * @return The proxy or null
     */
    public Proxy getProxy();

    /**
     * Returns the username that sessions built by this factory will connect
     * with.
     * 
     * @return The port
     */
    public String getUsername();

    /**
     * Returns a new session using the configured properties.
     * 
     * @return A new session
     * @throws JSchException
     *             If <code>username</code> or <code>hostname</code> are invalid
     * 
     * @see com.jcraft.jsch.JSch#getSession(String, String, int)
     */
    public Session newSession() throws JSchException;

    /**
     * Returns a builder for another session factory pre-initialized with the
     * configuration for this session factory.
     * 
     * @return A builder for a session factory
     */
    public SessionFactoryBuilder newSessionFactoryBuilder();

    abstract public class SessionFactoryBuilder {
        protected Map<String, String> config;
        protected String hostname;
        protected JSch jsch;
        protected int port;
        protected Proxy proxy;
        protected String username;

        protected SessionFactoryBuilder( JSch jsch, String username, String hostname, int port, Proxy proxy, Map<String, String> config ) {
            this.jsch = jsch;
            this.username = username;
            this.hostname = hostname;
            this.port = port;
            this.proxy = proxy;
            this.config = config;
        }

        /**
         * Replaces the current config with <code>config</code>
         * 
         * @param config
         *            The new config
         * @return This builder
         * 
         * @see com.pastdev.jsch.DefaultSessionFactory#setConfig(Map)
         */
        public SessionFactoryBuilder setConfig( Map<String, String> config ) {
            this.config = config;
            return this;
        }

        /**
         * Replaces the current hostname with <code>hostname</code>
         * 
         * @param hostname
         *            The new hostname
         * @return This builder
         */
        public SessionFactoryBuilder setHostname( String hostname ) {
            this.hostname = hostname;
            return this;
        }

        /**
         * Replaces the current port with <code>port</code>
         * 
         * @param port
         *            The new port
         * @return This builder
         */
        public SessionFactoryBuilder setPort( int port ) {
            this.port = port;
            return this;
        }

        /**
         * Replaces the current proxy with <code>proxy</code>
         * 
         * @param proxy
         *            The new proxy
         * @return This builder
         * 
         * @see com.pastdev.jsch.DefaultSessionFactory#setProxy(Proxy)
         */
        public SessionFactoryBuilder setProxy( Proxy proxy ) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Replaces the current username with <code>username</code>
         * 
         * @param username
         *            The new username
         * @return This builder
         */
        public SessionFactoryBuilder setUsername( String username ) {
            this.username = username;
            return this;
        }

        /**
         * Builds and returns a the new <code>SessionFactory</code> instance.
         * 
         * @return The built <code>SessionFactory</code>
         */
        abstract public SessionFactory build();
    }
}
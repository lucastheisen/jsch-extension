package com.pastdev.jsch;


import java.util.Map;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;


public interface SessionFactory {
    public static final int SSH_PORT = 22;

    public String getHostname();

    public int getPort();

    public String getUsername();

    public Session newSession() throws JSchException;

    public SessionFactoryBuilder newSessionFactoryBuilder() throws JSchException;

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
        
        public SessionFactoryBuilder setConfig( Map<String, String> config ) {
            this.config = config;
            return this;
        }

        public SessionFactoryBuilder setHostname( String hostname ) {
            this.hostname = hostname;
            return this;
        }

        public SessionFactoryBuilder setPort( int port ) {
            this.port = port;
            return this;
        }

        public SessionFactoryBuilder setProxy( Proxy proxy ) {
            this.proxy = proxy;
            return this;
        }

        public SessionFactoryBuilder setUsername( String username ) {
            this.username = username;
            return this;
        }

        abstract public SessionFactory build();
    }
}
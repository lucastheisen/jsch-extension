package com.pastdev.jsch.proxy;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import com.pastdev.jsch.SessionFactory;


public class SshProxy implements Proxy {
    private static Logger logger = LoggerFactory.getLogger( SshProxy.class );

    private Channel channel;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Session session;

    public SshProxy( SessionFactory sessionFactory ) throws JSchException {
        this.session = sessionFactory.newSession();
    }

    public void close() {
        if ( session != null && session.isConnected() ) {
            session.disconnect();
        }
    }

    public void connect( SocketFactory socketFactory, String host, int port, int timeout ) throws Exception {
        logger.debug( "connecting session" );
        session.connect();

        channel = session.getStreamForwarder( host, port );
        inputStream = channel.getInputStream();
        outputStream = channel.getOutputStream();

        channel.connect( timeout );
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public Socket getSocket() {
        return null;
    }

    @Override
    public String toString() {
        return "ssh proxy " + session.getUserName() + "@" + session.getHost() + " on port " + session.getPort();
    }
}

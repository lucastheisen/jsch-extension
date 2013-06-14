package com.pastdev.jsch.proxy;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import com.pastdev.jsch.IOUtils;


public class SshProxy implements Proxy {
    private static Logger logger = LoggerFactory.getLogger( SshProxy.class );

    private String hostname;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int port;
    private Session session;
    private SshProxySocket socket;
    private String username;

    public SshProxy( Session session ) {
        this.session = session;
    }

    public void connect( SocketFactory socketFactory, String host, int port, int timeout ) throws Exception {
        socket = new SshProxySocket( session );
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public Socket getSocket() {
        return socket;
    }

    public void close() {
        IOUtils.closeAndLogException( inputStream );
        IOUtils.closeAndLogException( outputStream );
        IOUtils.closeAndLogException( socket );
        inputStream = null;
        outputStream = null;
    }

    private class SshProxySocket extends Socket {
        private Channel channel;
        private InputStream inputStream;
        private OutputStream outputStream;
        private Session session;

        public SshProxySocket( Session session ) throws JSchException, IOException {
            this.session = session;

            if ( session.isConnected() ) {
                throw new IllegalStateException( "session must not be connected" );
            }

            logger.debug( "connecting session" );
            session.connect();

            String command = "";
            channel = session.openChannel( "exec" );
            logger.debug( "setting exec command to '{}'", command );
            ((ChannelExec)channel).setCommand( command );
            
            outputStream = channel.getOutputStream();
            inputStream = channel.getInputStream();

            logger.debug( "connecting channel" );
            channel.connect();
        }
        
        @Override
        public InputStream getInputStream() {
            return inputStream;
        }
        
        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }
        
        @Override
        public void close() {
            
        }
    }
}

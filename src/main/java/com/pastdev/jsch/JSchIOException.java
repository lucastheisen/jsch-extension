package com.pastdev.jsch;


import java.io.IOException;


import com.jcraft.jsch.JSchException;


public class JSchIOException extends IOException {
    private static final long serialVersionUID = 3262364212559255512L;

    public JSchIOException( JSchException e ) {
        super( e );
    }

    public JSchIOException( IOException e ) {
        super( e );
    }
    
    public JSchIOException( String message ) {
        super( new JSchException( message ) );
    }
}

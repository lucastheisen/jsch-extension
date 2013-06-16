package com.pastdev.jsch.scp;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DestinationOs;
import com.pastdev.jsch.IOUtils;
import com.pastdev.jsch.SessionFactory;


public class ScpFile {
    private DestinationOs os;
    private String[] path;
    private SessionFactory sessionFactory;

    public ScpFile( SessionFactory sessionFactory, String... path ) {
        this( sessionFactory, DestinationOs.UNIX, path );
    }

    public ScpFile( SessionFactory sessionFactory, DestinationOs os, String... path ) {
        this.sessionFactory = sessionFactory;
        this.os = os;
        this.path = path;
    }

    public void copyFrom( File file ) throws IOException, JSchException {
        copyFrom( file, null );
    }

    public void copyFrom( File file, String mode ) throws IOException, JSchException {
        FileInputStream from = null;
        ScpFileOutputStream to = null;
        try {
            from = new FileInputStream( file );
            to = mode == null
                    ? getOutputStream( file.length() )
                    : getOutputStream( file.length(), mode );
            IOUtils.copy( from, to );
        }
        finally {
            if ( from != null ) {
                IOUtils.closeAndLogException( from );
            }
            if ( to != null ) {
                IOUtils.closeAndLogException( to );
            }
        }

    }

    public void copyTo( File file ) throws JSchException, IOException {
        ScpFileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = getInputStream();
            String name = from.getName();
            String mode = from.getMode();
            if ( file.isDirectory() ) {
                file = new File( file, name );
            }
            to = new FileOutputStream( file );

            // attempt to set file mode... flakey in java 6 and below
            int userPerm = Character.getNumericValue( mode.charAt( 1 ) );
            int otherPerm = Character.getNumericValue( mode.charAt( 3 ) );
            if ( (userPerm & 1) == 1 ) {
                if ( (otherPerm & 1) == 1 ) {
                    file.setExecutable( true, false );
                }
                else {
                    file.setExecutable( true, true );
                }
            }
            if ( (userPerm & 2) == 2 ) {
                if ( (otherPerm & 2) == 2 ) {
                    file.setWritable( true, false );
                }
                else {
                    file.setWritable( true, true );
                }
            }
            if ( (userPerm & 4) == 4 ) {
                if ( (otherPerm & 4) == 4 ) {
                    file.setReadable( true, false );
                }
                else {
                    file.setReadable( true, true );
                }
            }

            IOUtils.copy( from, to );
        }
        finally {
            if ( from != null ) {
                IOUtils.closeAndLogException( from );
            }
            if ( to != null ) {
                IOUtils.closeAndLogException( to );
            }
        }
    }

    public void copyTo( ScpFile file ) throws JSchException, IOException {
        ScpFileInputStream from = null;
        ScpFileOutputStream to = null;
        try {
            from = getInputStream();
            String mode = from.getMode();
            long size = from.getSize();
            to = file.getOutputStream( size, mode );

            IOUtils.copy( from, to );
        }
        finally {
            if ( from != null ) {
                IOUtils.closeAndLogException( from );
            }
            if ( to != null ) {
                IOUtils.closeAndLogException( to );
            }
        }
    }

    public ScpFileInputStream getInputStream() throws JSchException, IOException {
        return new ScpFileInputStream( sessionFactory, getPath() );
    }

    public ScpFileOutputStream getOutputStream( long size ) throws JSchException, IOException {
        return getOutputStream( ScpEntry.newFile( getFilename(), size ) );
    }

    public ScpFileOutputStream getOutputStream( long size, String mode ) throws JSchException, IOException {
        return getOutputStream( ScpEntry.newFile( getFilename(), size, mode ) );
    }

    private ScpFileOutputStream getOutputStream( ScpEntry scpEntry ) throws JSchException, IOException {
        return new ScpFileOutputStream( sessionFactory, getDirectory(), scpEntry );
    }

    String getDirectory() {
        return os.joinPath( path, 0, path.length - 1 );
    }

    String getFilename() {
        return path[path.length - 1];
    }

    String getPath() {
        return os.joinPath( path, 0, path.length );
    }
}
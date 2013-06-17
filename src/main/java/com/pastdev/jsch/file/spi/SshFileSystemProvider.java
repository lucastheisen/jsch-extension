package com.pastdev.jsch.file.spi;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.file.DirectoryStream;
import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.attribute.BasicFileAttributes;


abstract public class SshFileSystemProvider implements Closeable {
    private CommandRunner commandRunner;
    private SessionFactory sessionFactory;

    public SshFileSystemProvider( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
        this.commandRunner = new CommandRunner( sessionFactory );
    }

    CommandRunner getCommandRunner() {
        return commandRunner;
    }

    public void close() throws IOException {
        commandRunner.close();
    }

    public InputStream getInputStream( SshPath path ) throws IOException {
        BasicFileAttributes attributes = readAttributes( path, BasicFileAttributes.class );

        StringBuilder command = new StringBuilder( "ssh -fq" );
        if ( attributes.isDirectory() ) {
            command.append( "r" );
        }
        command.append( " " ).append( path.toString() );

        try {
            commandRunner.open( command.toString() );
        }
        catch ( JSchException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public OutputStream getOutputStream( SshPath path ) throws IOException {
        return null;
    }

    abstract public String getSeparator();

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    abstract public DirectoryStream<SshPath> newDirectoryStream( SshPath path ) throws IOException;

    abstract public <A extends BasicFileAttributes> A readAttributes( SshPath path, Class<A> type ) throws IOException;

    abstract public Map<String, Object> readAttributes( SshPath path, String attributes ) throws IOException;

    public static class ArrayEntryDirectoryStream implements DirectoryStream<SshPath> {
        private String[] entries;
        private SshPath parent;

        public ArrayEntryDirectoryStream( SshPath parent, String[] entries ) {
            this.parent = parent;
            this.entries = entries;
        }

        public void close() throws IOException {
            // nothing to do...
        }

        public Iterator<SshPath> iterator() {
            return new Iterator<SshPath>() {
                private int currentIndex = 0;

                public boolean hasNext() {
                    return currentIndex < entries.length;
                }

                public SshPath next() {
                    return parent.resolve( entries[currentIndex++] );
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}

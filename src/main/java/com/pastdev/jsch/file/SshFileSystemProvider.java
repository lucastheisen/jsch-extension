package com.pastdev.jsch.file;


import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;


import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.command.CommandRunner;


abstract public class SshFileSystemProvider implements Closeable {
    private CommandRunner commandRunner;
    private SessionFactory sessionFactory;

    public SshFileSystemProvider( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
        this.commandRunner = new CommandRunner( sessionFactory );
    }

    protected CommandRunner getCommandRunner() {
        return commandRunner;
    }

    public void close() throws IOException {
        commandRunner.close();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    abstract public DirectoryStream<SshPath> newDirectoryStream( SshPath sshPath ) throws IOException;

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

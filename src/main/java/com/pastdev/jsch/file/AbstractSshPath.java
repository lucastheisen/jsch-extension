package com.pastdev.jsch.file;


import java.util.Arrays;
import java.util.Iterator;


import com.pastdev.jsch.SessionFactory;


abstract public class AbstractSshPath implements SshPath {
    private SshFileSystem fileSystem;
    private String[] parts;

    protected AbstractSshPath( SshFileSystem fileSystem, String first, String... more ) {
        this.fileSystem = fileSystem;
        this.parts = new String[more.length + 1];
        this.parts[0] = first;
        System.arraycopy( more, 0, this.parts, 1, more.length );
    }

    public SshFileSystem getFileSystem() {
        return fileSystem;
    }

    public String getHostname() {
        return fileSystem.provider().getSessionFactory().getHostname();
    }

    public String getName() {
        return parts[parts.length - 1];
    }

    public int getPort() {
        return fileSystem.provider().getSessionFactory().getPort();
    }

    public SshPath getParentPath() {
        if ( parts.length == 1 ) {
            return null;
        }
        else {
            int moreLength = parts.length - 2;
            String[] more = new String[moreLength];
            Arrays.copyOfRange( more, 1, moreLength );
            return fileSystem.getPath( parts[0], more );
        }
    }

    public String getUsername() {
        return fileSystem.provider().getSessionFactory().getUsername();
    }

    public Iterator<SshPath> iterator() {
        return new Iterator<SshPath>() {
            int index = 0;

            public boolean hasNext() {
                return index < parts.length;
            }

            public SshPath next() {
                if ( index++ == 0 ) {
                    return fileSystem.getPath( parts[0] );
                }
                else {
                    return fileSystem.getPath( parts[0], Arrays.copyOfRange( parts, 1, index ) );
                }
            }

            public void remove() {
                // path is immutable... dont want to allow changes
                throw new UnsupportedOperationException();
            }
        };
    }

    public SshPath resolve( String other ) {
        if ( parts.length == 1 ) {
            return fileSystem.getPath( parts[0] );
        }
        else {
            String[] more = new String[parts.length];
            int moreLength = parts.length - 1;
            Arrays.copyOfRange( more, 1, moreLength );
            more[moreLength] = other;
            return fileSystem.getPath( parts[0], more );
        }
    }

    public String toUri() {
        String path = joinParts( 0, parts.length );
        SessionFactory sessionFactory = fileSystem.provider().getSessionFactory();
        String separator = fileSystem.provider().getSeparator();
        return "ssh://" + sessionFactory.getUsername() + "@"
                + sessionFactory.getHostname() + ":" + sessionFactory.getPort()
                + separator
                + (path.startsWith( separator ) ? path : "~" + separator + path);
    }

    private String joinParts( int startIndex, int count ) {
        StringBuilder builder = new StringBuilder();
        int endIndex = startIndex + count;
        String separator = fileSystem.provider().getSeparator();
        for ( int i = startIndex; i < endIndex; i++ ) {
            if ( i > 0 ) {
                builder.append( separator );
            }
            builder.append( parts[i] );
        }
        return builder.toString();

    }

    @Override
    public String toString() {
        return joinParts( 0, parts.length );
    }
}

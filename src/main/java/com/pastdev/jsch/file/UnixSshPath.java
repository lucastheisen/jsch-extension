package com.pastdev.jsch.file;


import static com.pastdev.jsch.file.spi.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.util.Arrays;
import java.util.Iterator;


public class UnixSshPath extends AbstractSshPath {
    private boolean absolute;
    private String[] parts;

    UnixSshPath( UnixSshFileSystem unixSshFileSystem, String path ) {
        super( unixSshFileSystem );

        // normalize path string and discover separator indexes.
        // could probably optimize this at some point...
        if ( !path.isEmpty() ) {
            String[] parts = path.split( PATH_SEPARATOR + "+", 0 );
            if ( parts[0].isEmpty() ) {
                this.absolute = true;
                this.parts = Arrays.copyOfRange( parts, 1, parts.length - 1 );
                int newLength = parts.length - 1;
                this.parts = new String[newLength];
                System.arraycopy( parts, 1, this.parts, 0, newLength );
            }
            else {
                this.parts = parts;
            }
        }
    }

    private UnixSshPath( UnixSshFileSystem unixSshFileSystem, boolean isAbsolute, String... parts ) {
        super( unixSshFileSystem );
        this.absolute = isAbsolute;
        this.parts = parts == null ? new String[0] : parts;
    }

    public int compareTo( SshPath o ) {
        if ( !getFileSystem().provider().equals( o.getFileSystem().provider() ) ) {
            throw new ClassCastException( "cannot compare paths from 2 different provider instances" );
        }
        return toString().compareTo( ((UnixSshPath) o).toString() );
    }

    @Override
    public UnixSshFileSystem getFileSystem() {
        return (UnixSshFileSystem) super.getFileSystem();
    }

    public String getName( int index ) {
        if ( index < 0 ) {
            throw new IllegalArgumentException();
        }
        if ( index >= parts.length ) {
            throw new IllegalArgumentException();
        }

        return parts[index];
    }

    public int getNameCount() {
        return parts.length;
    }

    public SshPath getParent() {
        if ( parts.length == 0 && !isAbsolute() ) {
            return null;
        }
        if ( parts.length <= 1 ) {
            return new UnixSshPath( getFileSystem(), isAbsolute() );
        }
        return new UnixSshPath( getFileSystem(), isAbsolute(),
                Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
    }

    public SshPath getRoot() {
        if ( isAbsolute() ) {
            return new UnixSshPath( getFileSystem(), true );
        }
        else {
            return null;
        }
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public Iterator<SshPath> iterator() {
        return new Iterator<SshPath>() {
            int index = 0;

            public boolean hasNext() {
                return index < parts.length;
            }

            public SshPath next() {
                if ( index++ == 0 ) {
                    return getFileSystem().getPath( parts[0] );
                }
                else {
                    return getFileSystem().getPath( parts[0], Arrays.copyOfRange( parts, 1, index ) );
                }
            }

            public void remove() {
                // path is immutable... dont want to allow changes
                throw new UnsupportedOperationException();
            }
        };
    }

    public SshPath resolve( String other ) {
        String[] newPath = new String[parts.length + 1];
        System.arraycopy( parts, 0, newPath, 0, parts.length );
        newPath[parts.length] = other;
        return new UnixSshPath( getFileSystem(), isAbsolute(), newPath );
    }

    public SshPath toAbsolutePath() {
        if ( isAbsolute() ) {
            return this;
        }
        else {
            return getFileSystem().getPath(
                    getFileSystem().getDefaultDirectory() + PATH_SEPARATOR + toString() );
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for ( String part : parts ) {
            if ( builder.length() > 0 || isAbsolute() ) {
                builder.append( PATH_SEPARATOR );
            }
            builder.append( part );
        }

        return builder.toString();
    }
}

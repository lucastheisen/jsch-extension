package com.pastdev.jsch.file;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


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
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public SshPath getParentPath() {
        // TODO Auto-generated method stub
        return null;
    }

    abstract protected String getSeparator();

    public String getUsername() {
        // TODO Auto-generated method stub
        return null;
    }

    public Iterator<SshPath> iterator() {
        List<SshPath> ancestry = new ArrayList<SshPath>();
        SshPath parent = this;
        while ( (parent = parent.getParentPath() ) != null ) {
            ancestry.add( parent );
        }
        
        Collections.reverse( ancestry );
        
        return ancestry.iterator();
    }

    public SshPath resolve( String other ) {
        // TODO Auto-generated method stub
        return null;
    }

    public SshFile toFile() {
        // TODO Auto-generated method stub
        return null;
    }

    public String toUri() {
        String path = joinParts( 0, parts.length );
        SessionFactory sessionFactory = getFileSystem().provider().getSessionFactory();
        String separator = getSeparator();
        return "ssh://" + sessionFactory.getUsername() + "@"
                + sessionFactory.getHostname() + ":" + sessionFactory.getPort()
                + separator
                + (path.startsWith( separator ) ? path : "~" + separator + path);
    }

    private String joinParts( int startIndex, int count ) {
        StringBuilder builder = new StringBuilder();
        int endIndex = startIndex + count;
        for ( int i = startIndex; i < endIndex; i++ ) {
            if ( i > 0 ) {
                builder.append( getSeparator() );
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

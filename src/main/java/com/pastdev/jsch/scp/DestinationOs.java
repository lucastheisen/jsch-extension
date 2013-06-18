package com.pastdev.jsch.scp;

public enum DestinationOs {
    UNIX('/'),
    WINDOWS('\\');

    private char separator;

    private DestinationOs( char separator ) {
        this.separator = separator;
    }

    public String joinPath( String[] parts ) {
        return joinPath( parts, 0, parts.length );
    }

    public String joinPath( String[] parts, int start, int count ) {
        StringBuilder builder = new StringBuilder();
        for ( int i = start, end = start + count; i < end; i++ ) {
            if ( i > start ) {
                builder.append( separator );
            }
            builder.append( parts[i] );
        }
        return builder.toString();
    }
}
package com.pastdev.jsch.scp;

import java.io.IOException;
import java.util.regex.Pattern;


public class ScpEntry {
    private static final String DEFAULT_DIRECTORY_MODE = "0750";
    private static final String DEFAULT_FILE_MODE = "0640";
    private static final Pattern MODE_PATTERN = Pattern.compile( "[0-2]?[0-7]{3}" );

    private String mode;
    private String name;
    private long size;
    private Type type;
    
    private ScpEntry( String name, long size, String mode, Type type ) throws IOException {
        this.name = name;
        this.size = size;
        this.mode = type == Type.END_OF_DIRECTORY ? null : standardizeMode( mode );
        this.type = type;
    }
    
    public String getMode() {
        return mode;
    }
    
    public String getName() {
        return name;
    }
    
    public long getSize() {
        return size;
    }
    
    public boolean isDirectory() {
        return type == Type.DIRECTORY;
    }
    
    public boolean isEndOfDirectory() {
        return type == Type.END_OF_DIRECTORY;
    }
    
    public boolean isFile() {
        return type == Type.FILE;
    }
    
    public static ScpEntry newDirectory( String name ) throws IOException {
        return newDirectory( name, DEFAULT_DIRECTORY_MODE );
    }

    public static ScpEntry newDirectory( String name, String mode ) throws IOException {
        return new ScpEntry( name, 0L, mode, Type.DIRECTORY );
    }
    
    public static ScpEntry newEndOfDirectory() throws IOException {
        return new ScpEntry( null, 0L, null, Type.END_OF_DIRECTORY );
    }
    
    public static ScpEntry newFile( String name, long size ) throws IOException {
        return newFile( name, size, DEFAULT_FILE_MODE );
    }

    public static ScpEntry newFile( String name, long size, String mode ) throws IOException {
        return new ScpEntry( name, size, mode, Type.FILE );
    }

    private static String standardizeMode( String mode ) throws IOException {
        if ( !MODE_PATTERN.matcher( mode ).matches() ) {
            throw new IOException( "invalid file mode " + mode );
        }
        if ( mode.length() == 3 ) {
            mode = "0" + mode;
        }
        return mode;
    }
    
    @Override
    public String toString() {
        switch ( type ) {
            case FILE: return "C" + mode + " " + size + " " + name;
            case DIRECTORY: return "D" + mode + " " + size + " " + name;
            case END_OF_DIRECTORY: return "E";
            default: return "Weird, I have no idea how this happened...";
        }
    }
    
    public enum Type {
        FILE, DIRECTORY, END_OF_DIRECTORY
    }
}
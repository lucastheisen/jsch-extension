package com.pastdev.jsch.file.spi;


import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.attribute.PosixFileAttributes;


public class ScpEntry {
    private boolean isDirectory;
    private boolean isEndOfDirectory;
    private boolean isRegularFile;
    private String mode;
    private SshPath path;
    private long size;

    private ScpEntry() {
    }

    public static ScpEntry newEndOfDirectoryEntry() {
        ScpEntry entry = new ScpEntry();
        entry.isDirectory = false;
        entry.isEndOfDirectory = true;
        entry.isRegularFile = false;
        return entry;
    }

    public static ScpEntry newDirectoryEntry( SshPath path, String mode ) {
        ScpEntry entry = new ScpEntry();
        entry.path = path;
        entry.mode = mode;
        entry.isDirectory = true;
        entry.isEndOfDirectory = false;
        entry.isRegularFile = false;
        return entry;
    }

    public static ScpEntry newRegularFileEntry( SshPath path, String mode, long size ) {
        ScpEntry entry = new ScpEntry();
        entry.path = path;
        entry.mode = mode;
        entry.size = size;
        entry.isDirectory = false;
        entry.isEndOfDirectory = false;
        entry.isRegularFile = true;
        return entry;
    }

    public static ScpEntry newEntry( SshPath path, PosixFileAttributes attributes ) {
        ScpEntry entry = new ScpEntry();
        entry.path = path;
        entry.mode = attributes.permissions();
        entry.size = attributes.size();
        entry.isDirectory = attributes.isDirectory();
        entry.isEndOfDirectory = false;
        entry.isRegularFile = attributes.isRegularFile();
        return entry;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isEndOfDirectory() {
        return isEndOfDirectory;
    }

    public boolean isRegularFile() {
        return isRegularFile;
    }

    public String mode() {
        return mode;
    }

    public SshPath path() {
        return path;
    }

    public long size() {
        return size;
    }

    @Override
    public String toString() {
        if ( isEndOfDirectory ) {
            return "endOfDirectory";
        }
        if ( isDirectory ) {
            return "directory: " + path;
        }
        if ( isRegularFile ) {
            return "file: " + path;
        }
        else {
            throw new IllegalStateException( "unknown type of entry, THIS SHOULD NEVER HAPPEN" );
        }
    }
}

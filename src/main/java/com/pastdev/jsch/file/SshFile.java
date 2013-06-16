package com.pastdev.jsch.file;


import com.pastdev.jsch.scp.ScpFile;


abstract public class SshFile {
    private long lastModified;
    private long length;
    private SshPath path;
    private Type type;

    public String getName() {
        return path.getName();
    }
    
    public String getPath() {
        return path.toString();
    }

    public boolean isDirectory() {
        return type == Type.DIRECTORY;
    }

    public boolean isFile() {
        return type == Type.FILE;
    }

    public long lastModified() {
        return lastModified;
    }

    public long length() {
        return length;
    }

    public SshPath toPath() {
        return path;
    }

    private enum Type {
        FILE, DIRECTORY
    }
}

package com.pastdev.jsch.file.attribute;


public interface PosixFileAttributes extends BasicFileAttributes {
    public String group();

    public String owner();

    public String permissions();

}

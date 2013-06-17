package com.pastdev.jsch.file.attribute;


import java.util.Date;


public interface BasicFileAttributes {
    Date lastModifiedTime();

    Date lastAccessTime();

    Date creationTime();

    boolean isRegularFile();

    boolean isDirectory();

    boolean isSymbolicLink();

    boolean isOther();

    long size();

    Object fileKey();
}

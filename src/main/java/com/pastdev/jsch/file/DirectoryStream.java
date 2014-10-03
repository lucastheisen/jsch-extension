package com.pastdev.jsch.file;


import java.io.Closeable;
import java.util.Iterator;


public interface DirectoryStream<T> extends Iterable<T>, Closeable {
    public Iterator<T> iterator();
}

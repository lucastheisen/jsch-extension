package com.pastdev.jsch.file;


import java.net.URI;


public class SshPaths {
    public static SshPath getPath( String first, String... more ) {
        throw new UnsupportedOperationException( "ssh wont be default provider, so no use having this method" );
    }

    public static SshPath getPath( URI uri ) {
        return SshFileSystems.provider( uri ).getPath( uri );
    }
}

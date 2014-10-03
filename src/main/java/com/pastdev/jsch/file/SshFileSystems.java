package com.pastdev.jsch.file;


import java.io.IOException;
import java.net.URI;
import java.util.Map;


import com.pastdev.jsch.file.spi.SshFileSystemProvider;


public class SshFileSystems {
    public static SshFileSystem getSshFileSystem( URI uri ) {
        return provider( uri ).getSshFileSystem( uri );
    }

    public static SshFileSystem newSshFileSystem( URI uri, Map<String, ?> environment ) throws IOException {
        return provider( uri ).newSshFileSystem( uri, environment );
    }

    static SshFileSystemProvider provider( URI uri ) {
        for ( SshFileSystemProvider provider : SshFileSystemProvider.installedProviders() ) {
            if ( provider.getScheme().equalsIgnoreCase( uri.getScheme() ) ) {
                return provider;
            }
        }
        throw new RuntimeException( "provider not found for " + uri.getScheme() );
    }
}

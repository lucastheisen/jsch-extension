package com.pastdev.jsch.file;


import static com.pastdev.jsch.file.spi.UnixSshFileSystemProvider.PATH_SEPARATOR;


import com.pastdev.jsch.file.spi.UnixSshFileSystemProvider;


public class UnixSshFileSystem extends SshFileSystem {
    private String defaultDirectory;

    public UnixSshFileSystem( UnixSshFileSystemProvider provider, String defaultDirectory ) {
        super( provider );
        if ( defaultDirectory.charAt( 0 ) != PATH_SEPARATOR ) {
            throw new RuntimeException( "default directory must be absolute" );
        }
        this.defaultDirectory = defaultDirectory;
    }

    String getDefaultDirectory() {
        return defaultDirectory;
    }

    @Override
    public SshPath getPath( String first, String... more ) {
        if ( more == null || more.length == 0 ) return new UnixSshPath( this, first );

        StringBuilder builder = new StringBuilder( first );
        for ( String part : more ) {
            builder.append( PATH_SEPARATOR )
                    .append( part );
        }
        return new UnixSshPath( this, builder.toString() );
    }

    public String getSeparator() {
        return Character.toString( PATH_SEPARATOR );
    }
}

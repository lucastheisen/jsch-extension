package com.pastdev.jsch.file;


import static com.pastdev.jsch.file.spi.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.io.IOException;
import java.net.URI;


import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.file.spi.UnixSshFileSystemProvider;


public class UnixSshFileSystem extends SshFileSystem {
    private CommandRunner commandRunner;
    private String defaultDirectory;
    private URI uri;

    public UnixSshFileSystem( UnixSshFileSystemProvider provider, URI uri, CommandRunner commandRunner ) {
        super( provider );
        this.uri = uri;
        this.commandRunner = commandRunner;
        this.defaultDirectory = uri.getPath();

        if ( defaultDirectory.charAt( 0 ) != PATH_SEPARATOR ) {
            throw new RuntimeException( "default directory must be absolute" );
        }
    }

    public void close() throws IOException {
        commandRunner.close();
    }

    @Override
    public CommandRunner getCommandRunner() {
        return commandRunner;
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

    public URI getUri() {
        return uri;
    }
}

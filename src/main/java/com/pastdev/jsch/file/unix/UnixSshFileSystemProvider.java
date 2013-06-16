package com.pastdev.jsch.file.unix;


import java.io.IOException;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.file.DirectoryStream;
import com.pastdev.jsch.file.SshFileSystemProvider;
import com.pastdev.jsch.file.SshPath;


public class UnixSshFileSystemProvider extends SshFileSystemProvider {
    public UnixSshFileSystemProvider( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public DirectoryStream<SshPath> newDirectoryStream( final SshPath path ) throws IOException {
        CommandRunner commandRunner = getCommandRunner();
        try {
            int exitCode = commandRunner.execute( "ls -1 " + path.toString() );
            if ( exitCode == 0 ) {
                return new ArrayEntryDirectoryStream(
                        path, commandRunner.getStdout().split( "\n" ) );
            }
            else {
                throw new IOException( "failed to list directory (" + exitCode + "): " +
                        "out='" + commandRunner.getStderr() + "', " +
                        "err='" + commandRunner.getStderr() + "'" );
            }
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
        // "find " + path.toString() +
        // " -maxdepth 1 -type f -exec stat -c '%Y %a %s %F %n' {} +" );
    }
}

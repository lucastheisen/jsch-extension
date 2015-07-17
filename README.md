jsch-extension
==============
jsch-extension as an extension of the [JSch library](http://www.jcraft.com/jsch/) providing:
* A session factory for creating multiple sessions from the same configuration
* A proxy mechanism for the ssh connections allowing multi-hop tunneling
* A simplified command execution interface
* A simplified `sftp` interface
* A simplified `scp` interface
* Tunneling with simplified configuration and management

## Session Factory
A session factory is basically a container for configuration paired with a simple factory for creating `com.jcraft.jsch.Session` objects.  It is the core abstraction of the jsch-extension library.  The `DefaultSessionFactory` class is a default implementation providing useful configuration options.  For example:

```java
DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory( 
        username, hostname, port );
try {
    defaultSessionFactory.setKnownHosts( knownHosts );
    defaultSessionFactory.setIdentityFromPrivateKey( privateKey );
}
catch ( JSchException e ) {
    Assume.assumeNoException( e );
}

...

// new session using all defaults
Session session = defaultSessionFactory.newSession();

// new session with override for username
Session session2 = defaultSessionFactory.newSessionFactoryBuilder()
        .setUsername( "otheruser")
        .build()
        .newSession();
```

## Proxy Mechanism
The proxy allows for multi-hop ssh connections.  In other words, if you have a [bastion host](https://en.wikipedia.org/wiki/Bastion_host) type setup, you can tunnel thusly:


```java
SessionFactory proxySessionFactory = sessionFactory
        .newSessionFactoryBuilder()
        .setHostname( "foo" )
        .setPort( SessionFactory.SSH_PORT )
        .build();
SessionFactory destinationSessionFactory = sessionFactory
        .newSessionFactoryBuilder()
        .setProxy( new SshProxy( proxySessionFactory ) )
        .build();
Session session = destinationSessionFactory.newSession();
```

Which would ensure any connections to any session created by `destinationSessionFactory` would be tunneled through host `foo`.

## Simplified Command Execution
The simplified command execution is provided by the `CommandRunner`.  It makes execution of commands on remote systems as simple as:

```java
CommandRunner commandRunner = new CommandRunner( sessionFactory );
ExecuteResult result = commandRunner.execute( "ls -al" );
String filesInCurrentDirectory = result.getStdout();
```

## Simplified `sftp`
The simplified sftp is provided by the `SftpRunner`.  This allows direct access to `sftp` commands like this:

```java
SftpATTRS stat = null;
new SftpRunner( sessionFactory).execute( new Sftp() {
    @Override
    public void run( ChannelSftp sftp ) throws IOException {
        try {
            stat = sftp.lstat( path );
        }
        catch ( SftpException e ) {
        }
} );
```

## Simplified `scp`
The simplified `scp` is provided by the `ScpFile` class.  It allows you to copy to/from any file using:

```java
File toFile = new File( dir, toFilename );
try {
    ScpFile to = new ScpFile( sessionFactory, 
            "path", "to", "remote", "file" );
    to.copyFrom( new File( "/path/to/local/file" );
}
catch ( Exception e ) {
}
```

## Tunneling
Tunneling is provided by the classes in the `com.pastdev.jsch.tunnel` package.  There is support for plain tunneling as well as a convenient wrapper for `javax.sql.DataSource` objects.

### Plain tunneling
Opening a tunnel (equivalent to ssh port forwarding `-L foo:1234:bar:1234`) is as simple as:

```java
TunnelConnection tunnelConnection = new TunnelConnection( 
        sessionFactory,
        new Tunnel( "foo", 1234, "bar", 1234 ) );
tunnelConnection.open();
```

Plain tunneling also offers dynamic local port allocation.  Just supply `0` as the local port:

```java
TunnelConnection tunnelConnection = new TunnelConnection( 
        sessionFactory,
        new Tunnel( 0, "bar", 1234 ) );
tunnelConnection.open();
int assignedPort = tunnelConnection.getTunnel( "bar", 1234 )
        .getAssignedPort();
```

### Multiple tunnels
It is often necessary to tunnel multiple ports at the same time.  Perhaps you have a web server that you need access to both over http and remote desktop:

```java
TunnelConnectionManager manager = new TunnelConnectionManager( 
        sessionFactory,
        "127.0.0.2:80:webserver:80", 
        "127.0.0.2:13389:webserver:13389" );
manager.open();
```

### DataSource wrapper
The datasource wrapper comes in really handy when your database is locked down behind a firewall with no external connections allowed.  Instead you can use an ssh connection the the server and tunnel your database connection through it making it appear as if the connection is local:

```java
TunneledDataSourceWrapper wrapper = new TunneledDataSourceWrapper(
        new TunnelConnectionManager(
                sessionFactory,
                pathAndSpecList ),
        dataSource );
```

This wrapper is used exactly like any other `DataSource` and it will manage its own ssh tunnel opening and closing as necessary.
package com.pastdev.jsch.tunnel;


/**
 * Tunnel stores all the information needed to define an ssh port-forwarding
 * tunnel.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4254#section-7">rfc4254</a>
 */
public class Tunnel {
    private String spec;
    private String destinationHostname;
    private int destinationPort;
    private String localAlias;
    private int localPort;
    private int assignedLocalPort;

    /**
     * Creates a Tunnel from a <code>spec</code> string. For details on this
     * string, see {@link #getSpec()}.
     * <p>
     * Both <code>localAlias</code> and <code>localPort</code> are optional, in
     * which case they default to <code>localhost</code> and <code>0</code>
     * respectively.
     * </p>
     * <p>
     * Examples:
     * 
     * <pre>
     * // Equivalaent to new Tunnel(&quot;localhost&quot;, 0, &quot;foobar&quot;, 1234);
     * new Tunnel( &quot;foobar:1234&quot; );
     * // Equivalaent to new Tunnel(&quot;localhost&quot;, 1234, &quot;foobar&quot;, 1234);
     * new Tunnel( &quot;1234:foobar:1234&quot; );
     * // Equivalaent to new Tunnel(&quot;local_foobar&quot;, 1234, &quot;foobar&quot;, 1234);
     * new Tunnel( &quot;local_foobar:1234:foobar:1234&quot; );
     * </pre>
     * 
     * @param spec A tunnel spec string
     * 
     * @see #Tunnel(String, int, String, int)
     * @see <a href="http://tools.ietf.org/html/rfc4254#section-7">rfc4254</a>
     */
    public Tunnel( String spec ) {
        String[] parts = spec.split( ":" );
        if ( parts.length == 4 ) {
            this.localAlias = parts[0];
            this.localPort = Integer.parseInt( parts[1] );
            this.destinationHostname = parts[2];
            this.destinationPort = Integer.parseInt( parts[3] );
        }
        else if ( parts.length == 3 ) {
            this.localPort = Integer.parseInt( parts[0] );
            this.destinationHostname = parts[1];
            this.destinationPort = Integer.parseInt( parts[2] );
        }
        else {
            this.localPort = 0; // dynamically assigned port
            this.destinationHostname = parts[0];
            this.destinationPort = Integer.parseInt( parts[1] );
        }
    }

    /**
     * Creates a Tunnel to <code>destinationPort</code> on
     * <code>destinationHostname</code> from a dynamically assigned port on
     * <code>localhost</code>. Simply calls
     * 
     * @param destinationHostname
     *            The hostname to tunnel to
     * @param destinationPort
     *            The port to tunnel to
     * 
     * @see #Tunnel(int, String, int)
     * @see <a href="http://tools.ietf.org/html/rfc4254#section-7">rfc4254</a>
     */
    public Tunnel( String destinationHostname, int destinationPort ) {
        this( 0, destinationHostname, destinationPort );
    }

    /**
     * Creates a Tunnel to <code>destinationPort</code> on
     * <code>destinationHostname</code> from <code>localPort</code> on
     * <code>localhost</code>.
     * 
     * @param localPort
     *            The local port to bind to
     * @param destinationHostname
     *            The hostname to tunnel to
     * @param destinationPort
     *            The port to tunnel to
     * 
     * @see #Tunnel(String, int, String, int)
     * @see <a href="http://tools.ietf.org/html/rfc4254#section-7">rfc4254</a>
     */
    public Tunnel( int localPort, String destinationHostname, int destinationPort ) {
        this( null, localPort, destinationHostname, destinationPort );
    }

    /**
     * Creates a Tunnel to <code>destinationPort</code> on
     * <code>destinationHostname</code> from <code>localPort</code> on
     * <code>localAlias</code>.
     * <p>
     * This is similar in behavior to the <code>-L</code> option in ssh, with
     * the exception that you can specify <code>0</code> for the local port in
     * which case the port will be dynamically allocated and you can
     * {@link #getAssignedLocalPort()} after the tunnel has been started.
     * </p>
     * <p>
     * A common use case for <code>localAlias</code> might be to link your
     * loopback interfaces to names via an entries in <code>/etc/hosts</code>
     * which would allow you to use the same port number for more than one
     * tunnel. For example:
     * 
     * <pre>
     * 127.0.0.2 foo
     * 127.0.0.3 bar
     * </pre>
     * 
     * Would allow you to have both of these open at the same time:
     * 
     * <pre>
     * new Tunnel( &quot;foo&quot;, 1234, &quot;remote_foo&quot;, 1234 );
     * new Tunnel( &quot;bar&quot;, 1234, &quot;remote_bar&quot;, 1234 );
     * </pre>
     * 
     * @param localAlias
     *            The local interface to bind to
     * @param localPort
     *            The local port to bind to
     * @param destinationHostname
     *            The hostname to tunnel to
     * @param destinationPort
     *            The port to tunnel to
     * 
     * @see com.jcraft.jsch.Session#setPortForwardingL(String, int, String, int)
     * @see <a href="http://tools.ietf.org/html/rfc4254#section-7">rfc4254</a>
     */
    public Tunnel( String localAlias, int localPort, String destinationHostname, int destinationPort ) {
        this.localAlias = localAlias;
        this.localPort = localPort;
        this.destinationHostname = destinationHostname;
        this.destinationPort = destinationPort;
    }

    /**
     * Returns true if <code>other</code> is a Tunnel whose <code>spec</code>
     * (either specified or calculated) is equal to this tunnels
     * <code>spec</code>.
     * 
     * @return True if both tunnels have equivalent <code>spec</code>'s
     * 
     * @see #getSpec()
     */
    @Override
    public boolean equals( Object other ) {
        return (other instanceof Tunnel) &&
                getSpec().equals( ((Tunnel) other).getSpec() );
    }

    /**
     * Returns the local port currently bound to. If <code>0</code> was
     * specified as the port to bind to, this will return the dynamically
     * allocated port, otherwise it will return the port specified.
     * 
     * @return The local port currently bound to
     */
    public int getAssignedLocalPort() {
        return assignedLocalPort == 0 ? localPort : assignedLocalPort;
    }

    /**
     * Returns the hostname of the destination.
     * 
     * @return The hostname of the destination
     */
    public String getDestinationHostname() {
        return destinationHostname;
    }

    /**
     * Returns the port of the destination.
     * 
     * @return The port of the destination
     */
    public int getDestinationPort() {
        return destinationPort;
    }

    /**
     * Returns the local alias bound to. See <a
     * href="http://tools.ietf.org/html/rfc4254#section-7">rfc4254</a> for
     * details on acceptible values.
     * 
     * @return The local alias bound to
     */
    public String getLocalAlias() {
        return localAlias;
    }

    /**
     * Returns the port this tunnel was configured with. If you want to get the
     * runtime port, use {@link #getAssignedLocalPort()}.
     * 
     * @return The port this tunnel was configured with
     */
    public int getLocalPort() {
        return localPort;
    }

    /**
     * Returns the spec string (either calculated or specified) for this tunnel.
     * <p>
     * A spec string is composed of 4 parts separated by a colon (<code>:</code>
     * ):
     * <ol>
     * <li><code>localAlias</code> (<i>optional</i>)</li>
     * <li><code>localPort</code> (<i>optional</i>)</li>
     * <li><code>destinationHostname</code></li>
     * <li><code>destinationPort</code></li>
     * </ol>
     * 
     * @return The spec string
     */
    public String getSpec() {
        if ( spec == null ) {
            spec = toString().toLowerCase();
        }
        return spec;
    }

    @Override
    public int hashCode() {
        return getSpec().hashCode();
    }

    void setAssignedLocalPort( int port ) {
        this.assignedLocalPort = port;
    }

    @Override
    public String toString() {
        return (localAlias == null ? "" : localAlias + ":")
                + (assignedLocalPort == 0
                        ? localPort
                        : ("(0)" + assignedLocalPort))
                + ":" + destinationHostname + ":" + destinationPort;
    }
}

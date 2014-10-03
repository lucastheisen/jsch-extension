package com.pastdev.jsch.tunnel;


public class Tunnel {
    private String spec;
    private String destinationHostname;
    private int destinationPort;
    private String localAlias;
    private int localPort;
    private int assignedLocalPort;

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

    public Tunnel( String destinationHostname, int destinationPort ) {
        this( 0, destinationHostname, destinationPort );
    }

    public Tunnel( int localPort, String destinationHostname, int destinationPort ) {
        this( null, localPort, destinationHostname, destinationPort );
    }

    public Tunnel( String localAlias, int localPort, String destinationHostname, int destinationPort ) {
        this.localAlias = localAlias;
        this.localPort = localPort;
        this.destinationHostname = destinationHostname;
        this.destinationPort = destinationPort;
    }

    @Override
    public boolean equals( Object other ) {
        return (other instanceof Tunnel) &&
                getSpec().equals( ((Tunnel) other).getSpec() );
    }

    public int getAssignedLocalPort() {
        return assignedLocalPort == 0 ? localPort : assignedLocalPort;
    }

    public String getDestinationHostname() {
        return destinationHostname;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public String getLocalAlias() {
        return localAlias;
    }

    public int getLocalPort() {
        return localPort;
    }

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

    public void setAssignedLocalPort( int port ) {
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

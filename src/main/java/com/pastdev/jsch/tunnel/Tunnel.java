package com.pastdev.jsch.tunnel;


public class Tunnel {
    private String spec;
    private String destinationHostname;
    private int destinationPort;
    private String localAlias;
    private int localPort;

    public Tunnel( String spec ) {
        String[] parts = spec.split( ":" );
        if ( parts.length == 4 ) {
            this.localAlias = parts[0];
            this.localPort = Integer.parseInt( parts[1] );
            this.destinationHostname = parts[2];
            this.destinationPort = Integer.parseInt( parts[3] );
        }
        else {
            this.localPort = Integer.parseInt( parts[0] );
            this.destinationHostname = parts[1];
            this.destinationPort = Integer.parseInt( parts[2] );
        }
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
                getSpec().equals( ((Tunnel)other).getSpec() );
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

    @Override
    public String toString() {
        return (localAlias == null ? "" : localAlias + ":") + 
                localPort + ":" + destinationHostname + ":" + destinationPort;
    }
}

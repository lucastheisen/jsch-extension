package com.pastdev.jsch.tunnel;


public class Tunnel {
    private String destinationHostname;
    private int destinationPort;
    private int localPort;

    public Tunnel( int localPort, String destinationHostname, int destinationPort ) {
        this.localPort = localPort;
        this.destinationHostname = destinationHostname;
        this.destinationPort = destinationPort;
    }
    
    public String getDestinationHostname() {
        return destinationHostname;
    }
    
    public int getDestinationPort() {
        return destinationPort;
    }
    
    public int getLocalPort() {
        return localPort;
    }

    @Override
    public String toString() {
        return localPort + ":" + destinationHostname + ":" + destinationPort;
    }
}

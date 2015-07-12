package com.pastdev.jsch.tunnel;


import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;


import javax.sql.DataSource;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TunneledDataSourceWrapper implements DataSource, Closeable {
    private static Logger log = LoggerFactory.getLogger( TunneledDataSourceWrapper.class );
    private DataSource dataSource;
    private TunnelConnectionManager tunnel;

    public TunneledDataSourceWrapper( TunnelConnectionManager tunnel, DataSource dataSource ) {
        this.tunnel = tunnel;
        this.dataSource = dataSource;
    }

    @Override
    public void close() throws IOException {
        log.info( "closing tunnel" );
        tunnel.close();
    }

    private void ensureTunnelIsOpen() throws SQLException {
        try {
            tunnel.ensureOpen();
        }
        catch ( Exception e ) {
            throw new SQLException( "unable to open tunnel", e );
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        ensureTunnelIsOpen();
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection( String username, String password ) throws SQLException {
        ensureTunnelIsOpen();
        return dataSource.getConnection( username, password );
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    	return dataSource.getParentLogger();
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) throws SQLException {
        if ( dataSource.getClass().equals( iface.getClass() ) ) return true;
        return dataSource.isWrapperFor( iface );
    }

    @Override
    public void setLoginTimeout( int seconds ) throws SQLException {
        dataSource.setLoginTimeout( seconds );
    }

    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException {
        dataSource.setLogWriter( out );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if ( dataSource.getClass().equals( iface.getClass() ) ) return (T) dataSource;
        return dataSource.unwrap( iface );
    }
}

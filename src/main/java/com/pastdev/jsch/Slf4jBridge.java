package com.pastdev.jsch;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Bridges all JSch logging to the SLF4J API.
 */
public class Slf4jBridge implements com.jcraft.jsch.Logger {
    private static Logger logger = LoggerFactory.getLogger( Slf4jBridge.class );

    public boolean isEnabled( int level ) {
        switch ( level ) {
            case com.jcraft.jsch.Logger.DEBUG:
                return logger.isDebugEnabled();
            case com.jcraft.jsch.Logger.INFO:
                return logger.isInfoEnabled();
            case com.jcraft.jsch.Logger.WARN:
                return logger.isWarnEnabled();
            case com.jcraft.jsch.Logger.ERROR:
                return logger.isErrorEnabled();
            case com.jcraft.jsch.Logger.FATAL:
                return true;
            default:
                return logger.isTraceEnabled();
        }
    }

    public void log( int level, String message ) {
        switch ( level ) {
            case com.jcraft.jsch.Logger.DEBUG:
                logger.debug( message );
                break;
            case com.jcraft.jsch.Logger.INFO:
                logger.info( message );
                break;
            case com.jcraft.jsch.Logger.WARN:
                logger.warn( message );
                break;
            case com.jcraft.jsch.Logger.ERROR:
                logger.error( message );
                break;
            case com.jcraft.jsch.Logger.FATAL:
                logger.error( message );
                break;
            default:
                logger.trace( message );
                break;
        }
    }

}

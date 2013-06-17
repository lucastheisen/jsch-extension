package com.pastdev.jsch.file.spi;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.file.DirectoryStream;
import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.attribute.BasicFileAttributes;
import com.pastdev.jsch.file.attribute.PosixFileAttributes;


public class UnixSshFileSystemProvider extends SshFileSystemProvider {
    private static final String ASCII_UNIT_SEPARATOR = Character.toString( (char)31 );

    public UnixSshFileSystemProvider( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public String getSeparator() {
        return "/";
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes( SshPath path, Class<A> type ) throws IOException {
        if ( type == BasicFileAttributes.class ) {
            return (A)new BasicFileAttributesImpl( path );
        }
        if ( type == PosixFileAttributes.class ) {
            return (A)new PosixFileAttributesImpl( path );
        }
        if ( type == null ) {
            throw new NullPointerException();
        }
        return (A)null;
    }

    @Override
    public Map<String, Object> readAttributes( SshPath path, String attributes ) throws IOException {
        List<SupportedAttribute> attributeList = new ArrayList<SupportedAttribute>();
        for ( String attributeName : attributes.split( "," ) ) {
            attributeName = attributeName.trim();
            if ( attributeName.equals( "*" ) ) {
                return readAttributes( path, SupportedAttribute.values() );
            }
            SupportedAttribute attribute = SupportedAttribute.fromString( attributeName );
            if ( attribute != null ) {
                attributeList.add( attribute );
            }
        }
        return readAttributes( path, attributeList.toArray( new SupportedAttribute[attributeList.size()] ) );
    }

    private Map<String, Object> readAttributes( SshPath path, SupportedAttribute[] attributes ) throws IOException {
        StringBuilder commandBuilder = new StringBuilder( "stat --printf \"" );
        for ( int i = 0; i < attributes.length; i++ ) {
            if ( i > 0 ) {
                commandBuilder.append( ASCII_UNIT_SEPARATOR );
            }
            commandBuilder.append( attributes[i].option() );
        }
        commandBuilder.append( "\" " ).append( path.toString() );

        CommandRunner commandRunner = getCommandRunner();
        try {
            int exitCode = commandRunner.execute( commandBuilder.toString() );
            if ( exitCode == 0 ) {
                String[] values = commandRunner.getStdout().split( ASCII_UNIT_SEPARATOR );
                Map<String, Object> map = new HashMap<String, Object>();
                int index = 0;
                for ( SupportedAttribute attribute : attributes ) {
                    map.put( attribute.name(), attribute.toObject( values[index++] ) );
                }
                return map;
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
    }

    private enum SupportedAttribute {
        creationTime("%W", Date.class),
        group("%G", String.class),
        fileKey("%i", Long.TYPE),
        lastAccessTime("%X", Date.class),
        lastModifiedTime("%Y", Date.class),
        lastChangedTime("%Z", Date.class),
        name("%n", String.class),
        owner("%U", String.class),
        permissions("%a", String.class),
        size("%s", Long.TYPE),
        isRegularFile("%F", Boolean.TYPE),
        isDirectory("%F", Boolean.TYPE),
        isSymbolicLink("%F", Boolean.TYPE),
        isOther("%F", Boolean.TYPE);

        private static Map<String, SupportedAttribute> lookup;

        static {
            lookup = new HashMap<String, SupportedAttribute>();
            for ( SupportedAttribute attribute : values() ) {
                lookup.put( attribute.name(), attribute );
            }
        }

        private String option;
        private Class<?> valueClass;

        private SupportedAttribute( String option, Class<?> valueClass ) {
            this.option = option;
            this.valueClass = valueClass;
        }

        public static SupportedAttribute fromString( String attribute ) {
            return lookup.get( attribute );
        }

        public String option() {
            return option;
        }

        public Object toObject( String value ) {
            if ( this == isRegularFile ) {
                return "regular file".equals( value.toLowerCase() );
            }
            if ( this == isDirectory ) {
                return "directory".equals( value.toLowerCase() );
            }
            if ( this == isSymbolicLink ) {
                return "symbolic link".equals( value.toLowerCase() );
            }
            if ( this == isOther ) {
                return "other".equals( value.toLowerCase() );
            }
            if ( valueClass == Long.TYPE ) {
                return Long.parseLong( value );
            }
            if ( valueClass == Date.class ) {
                return new Date( Long.parseLong( value ) * 1000 );
            }
            else {
                return value;
            }
        }
    }

    private static final SupportedAttribute[] BASIC_SUPPORTED_ATTRIBUTES = new SupportedAttribute[] {
            SupportedAttribute.creationTime,
            SupportedAttribute.fileKey,
            SupportedAttribute.isDirectory,
            SupportedAttribute.isRegularFile,
            SupportedAttribute.isSymbolicLink,
            SupportedAttribute.isOther,
            SupportedAttribute.lastAccessTime,
            SupportedAttribute.lastModifiedTime,
            SupportedAttribute.size };
    private static final SupportedAttribute[] POSIX_ADDITIONAL_SUPPORTED_ATTRIBUTES = new SupportedAttribute[] {
            SupportedAttribute.permissions,
            SupportedAttribute.owner,
            SupportedAttribute.group };

    private class BasicFileAttributesImpl implements BasicFileAttributes {
        protected Map<String, Object> map;

        public BasicFileAttributesImpl( SshPath path ) throws IOException {
            this( path, null );
        }

        protected BasicFileAttributesImpl( SshPath path, SupportedAttribute[] additionalAttributes ) throws IOException {
            SupportedAttribute[] supportedAttributes = null;
            if ( additionalAttributes == null ) {
                supportedAttributes = BASIC_SUPPORTED_ATTRIBUTES;
            }
            else {
                supportedAttributes = new SupportedAttribute[BASIC_SUPPORTED_ATTRIBUTES.length + additionalAttributes.length];
                System.arraycopy( BASIC_SUPPORTED_ATTRIBUTES, 0, supportedAttributes, 0, BASIC_SUPPORTED_ATTRIBUTES.length );
                System.arraycopy( additionalAttributes, 0, supportedAttributes, BASIC_SUPPORTED_ATTRIBUTES.length, additionalAttributes.length );
            }
            map = readAttributes( path, supportedAttributes );
        }

        public Date creationTime() {
            return (Date)map.get( SupportedAttribute.creationTime.toString() );
        }

        public Object fileKey() {
            return map.get( SupportedAttribute.fileKey.toString() );
        }

        public boolean isDirectory() {
            return (Boolean)map.get( SupportedAttribute.isDirectory.toString() );
        }

        public boolean isOther() {
            return (Boolean)map.get( SupportedAttribute.isOther.toString() );
        }

        public boolean isRegularFile() {
            return (Boolean)map.get( SupportedAttribute.isRegularFile.toString() );
        }

        public boolean isSymbolicLink() {
            return (Boolean)map.get( SupportedAttribute.isSymbolicLink.toString() );
        }

        public Date lastAccessTime() {
            return (Date)map.get( SupportedAttribute.lastAccessTime.toString() );
        }

        public Date lastModifiedTime() {
            return (Date)map.get( SupportedAttribute.lastModifiedTime.toString() );
        }

        public long size() {
            return (Long)map.get( SupportedAttribute.size.toString() );
        }
    }

    private class PosixFileAttributesImpl extends BasicFileAttributesImpl implements PosixFileAttributes {
        public PosixFileAttributesImpl( SshPath path ) throws IOException {
            super( path, POSIX_ADDITIONAL_SUPPORTED_ATTRIBUTES );
        }

        public String group() {
            return (String)map.get( SupportedAttribute.group.toString() );
        }

        public String owner() {
            return (String)map.get( SupportedAttribute.owner.toString() );
        }

        public String permissions() {
            return (String)map.get( SupportedAttribute.permissions.toString() );
        }
    }
}

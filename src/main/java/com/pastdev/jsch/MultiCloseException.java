package com.pastdev.jsch;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MultiCloseException extends IOException {
    private static final long serialVersionUID = -8654074724588491465L;
    
    private List<Exception> causes;

    public void add( Exception e ) {
        if ( causes == null ) {
            causes = new ArrayList<Exception>();
        }
        causes.add( e );
    }

    public List<Exception> getCauses() {
        return causes;
    }

}

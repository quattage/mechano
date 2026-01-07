package com.quattage.mechano.foundation;

public interface Disposable {

    static void dispose(Object obj) {
        if(obj instanceof Disposable dobj) dobj.dispose();
    }

    void dispose();
}

package com.quattage.mechano.foundation;

public interface Disposable {

    static void dispose(Object obj) {
        if(obj instanceof Disposable dobj) dobj.dispose();
    }

    static boolean hasBeenDisposed(Object obj) {
        return obj instanceof Disposable dp ? dp.hasBeenDisposed() : false;
    }

    default void assertNotDisposed() {
        if(hasBeenDisposed()) 
            throw new IllegalStateException("An operation failed on object '" + this.getClass().getSimpleName() + "' because this object was disposed.");
    }

    void dispose();

    boolean hasBeenDisposed();
}

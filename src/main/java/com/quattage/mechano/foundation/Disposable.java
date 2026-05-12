package com.quattage.mechano.foundation;

public interface Disposable {

    static void disposeOf(Object obj) {
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

    /**
     * Indicates whether or not a disposable object has been disposed of.
     * Implementations may return <code>true</code> here to indicate that 
     * this object is still usable.
     * @return <code>false</code> if this object can't be used because it has
     * been disposed of.
     */
    boolean hasBeenDisposed();
}

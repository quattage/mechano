package com.quattage.mechano.foundation.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import com.quattage.mechano.Mechano;

/**
 * A wrapper for a primitive array that sorts nulls to the end
 * whenever it is changed.
 */
public class NullSortedArray<T> implements Collection<T> {

    private final T[] backingArray;
    private final BiConsumer<T, Boolean> updateAction;
    private int size;

    @SuppressWarnings("unchecked")
    public NullSortedArray(int absoluteSize, BiConsumer<T, Boolean> updateAction) {
        if(absoluteSize < 0) 
            throw new IllegalArgumentException("Error instantiating NullSortedArray - A size of " + absoluteSize + " is invalid! (must be at least 1!)");
        this.backingArray = (T[]) new Object[absoluteSize];
        this.updateAction = updateAction;
    }

    public NullSortedArray(int absoluteSize) {
        this(absoluteSize, null);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return backingArray[0] == null;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) > -1;
    }

    public int indexOf(Object o) {

        for(int x = 0; x < backingArray.length; x++) {
            if(backingArray[x] == null) break;
            if(backingArray[x].equals(o)) return x;
        }

        return -1;
    }

    public T get(int index) {
        if(index < 0 || index >= backingArray.length)
            throw new ArrayIndexOutOfBoundsException("Index " + index + " is out of bounds for NullSortedArray of length " + backingArray.length + "!");
        return backingArray[index];
    }

    /**
     * don't use this
     */
    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(backingArray).iterator();
    }

    @Override
    public Object[] toArray() {
        return backingArray;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        return (T[]) backingArray;
    }

    @Override
    public boolean add(T e) {
        for(int x = 0; x < backingArray.length; x++) {
            if(backingArray[x] == null) {
                backingArray[x] = e;
                size++;
                onUpdated(e, true);
                return true;
            }

            if(backingArray[x].equals(e)) {
                backingArray[x] = e;
                onUpdated(e, true);
                return false;
            }

            continue;
        }

        return true;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if(index > -1) {
            T temp = backingArray[index];
            backingArray[index] = null;
            sortNulls();
            size--;
            onUpdated(temp, false);
            return true;
        }

        return false;
    }

    /**
     * Removes objects by equivalence with a predicate.
     * @param o Object to remove
     * @param equivalence Predicate for comparison. If it returns <code>true</code>, the object will be removed.
     * @param bailout if <code>true</code>, iteration will be stopped at the first object found.
     * @return
     */
    public <R> boolean removeBy(R o, BiPredicate<T, R> equivalence, boolean bailout) {
        
        boolean changed = false;

        for(int x = 0; x < backingArray.length; x++) {
            T temp = backingArray[x];
            if(temp == null) break;

            if(equivalence.test(temp, o)) {
                changed = true;
                backingArray[x] = null;
                size--;
                onUpdated(temp, false);

                if(bailout) {
                    sortNulls();
                    return true;
                }
            }
        }

        if(changed)
            sortNulls();

        return changed;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for(Object o : c) 
            if(!contains(o)) return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for(T o : c) {
            if(isFull()) break;
            if(add(o)) changed = true;
        }
        return changed;
    }

    public boolean isFull() {
        return size == backingArray.length;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeAll'");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retainAll'");
    }

    @Override
    public void clear() {
        for(int x = 0; x < backingArray.length; x++)
            backingArray[x] = null;
        size = 0;
    }

    private void sortNulls() {
        int lastIndex = backingArray.length - 1;
        int firstIndex = 0;
        while(firstIndex <= lastIndex) {
            if(backingArray[firstIndex] != null)
                firstIndex++;
            else if(backingArray[lastIndex] == null)
                lastIndex--;
            else {
                backingArray[firstIndex] = backingArray[lastIndex];
                backingArray[lastIndex] = null;
            }
        }
    }

    public String toString() {

        String out = "NullSortedArray (length: " + size + ", [";
        for(int x = 0; x < backingArray.length; x++) {
            out += "(" + backingArray[x] + ")" + (x < backingArray.length - 1 ? ", " : " ");
        }
        return out + "])";
    }

    private void onUpdated(T object, boolean addOrRemove) {
        if(updateAction == null) return;
        updateAction.accept(object, addOrRemove);
    }
}
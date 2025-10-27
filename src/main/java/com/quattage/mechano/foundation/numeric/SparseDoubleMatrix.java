package com.quattage.mechano.foundation.numeric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.apache.logging.log4j.util.TriConsumer;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * A sparse matrix of double values with a high level of 
 * control over its internal table. This class makes heavy 
 * use of unimi fastutil's {@link Int2DoubleOpenHashMap} to 
 * optimize memory footprint. To that end, matrices can be 
 * can be {@link #trim trimmed} and  {@link #minimize minimized} 
 * whenever necessary to omit zero or near-zero values from 
 * the table. Modifying operations ({@link #add}, {@link #subtract},
 * {@link #operate}, and {@link #setValue}) will
 * avoid storing values that are functionally equivalent to zero.
 */
public class SparseDoubleMatrix {

    private final double EPSILON = 0.00001d;
    
    private int rows;
    private int cols;
    private final Int2ObjectOpenHashMap<Int2DoubleOpenHashMap> elements;

    public SparseDoubleMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.elements = new Int2ObjectOpenHashMap<Int2DoubleOpenHashMap>();
    }

    public double setValue(int row, int col, double value) {
        assertInBounds(row, col);
        boolean isZero = isEquivalentToZero(value);
        Int2DoubleOpenHashMap trgtRow = elements.get(row);
        if(trgtRow == null) {
            if(isZero) return 0;
            trgtRow = new Int2DoubleOpenHashMap(3);
            elements.put(row, trgtRow);
        }
        return isZero ? trgtRow.remove(col) : trgtRow.put(col, value);
    }

    public double clearValue(int row, int col) {
        assertInBounds(row, col);
        Int2DoubleOpenHashMap trgtRow = elements.get(row);
        if(trgtRow == null) return 0;
        double out = trgtRow.remove(col);
        if(trgtRow.isEmpty()) elements.remove(row);
        return out;
    }

    public double get(int row, int col) {
        assertInBounds(row, col);
        final Int2DoubleOpenHashMap acq = elements.get(row);
        if(acq == null) return 0;
        return acq.getOrDefault(col, 0d);
    }

    public @Nullable Int2DoubleOpenHashMap getRow(int row) {
        if(row < 0 || row >= rows) return null;
        return elements.get(row);
    }

    /**
     * Adds the provided matrix to this one, modifying this one as a result. 
     * @param other Other matrix to add. Must have the same dimensions as this one.
     * @return This matrix, modified to reflect the changes of this operation
     */
    public SparseDoubleMatrix add(SparseDoubleMatrix other) {
        assertSameSizeAs(other);
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                double result = this.get(row, col) + other.get(row, col);
                if(isEquivalentToZero(result)) this.clearValue(row, col);
                else this.setValue(row, col, result);
            }
        }
        return this;
    }

    /**
     * Subtracts the provided matrix from this one, modyfing this one as a result.
     * @param other Other matrix to subtract. Must have the same dimensions as this one.
     * @return This matrix, modified to reflect the changes of this operation
     */
    public SparseDoubleMatrix subtract(SparseDoubleMatrix other) {
        assertSameSizeAs(other);
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                double result = this.get(row, col) - other.get(row, col);
                if(isEquivalentToZero(result)) this.clearValue(row, col);
                else this.setValue(row, col, result);
            }
        }
        return this;
    }

    /**
     * Runs <code>operation</code> on every member of <code>this</code> and <code>other</code>.
     * The return value of <code>operation</code> will be stored in this matrix.
     * @param other Other matrix to operate with. Must have the same dimensions as this one.
     * @return This matrix, modified to reflect the changes of this operation
     */
    public SparseDoubleMatrix operate(SparseDoubleMatrix other, BiFunction<Double, Double, Double> operation) {
        assertSameSizeAs(other);
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                double result = operation.apply(this.get(row, col), other.get(row, col));
                if(result < EPSILON) this.clearValue(row, col);
                else this.setValue(row, col, result);
            }
        }
        return this;
    }

    public void growToFit(int totalRows, int totalCols) { growToFit(totalRows, totalCols, true); }

    /**
     * Grows or shrinks this matrix so that its dimensions 
     * are equal to the provided ones.
     * @param totalRows the new amount of rows (x dimension) to store
     * @param totalCols the new amount of columns (y dimension) to store
     * @param allocate (Optional, defaults to <code>true</code>) If <code>
     * true</code>, the underlying table will have its capacity altered to
     * fit the provided size
     */
    public void growToFit(int totalRows, int totalCols, boolean allocate) {
        this.rows = totalCols;
        this.cols = totalCols;
        if(!allocate) return;
        elements.ensureCapacity(rows);
        for(Int2DoubleOpenHashMap col : elements.values())
            col.ensureCapacity(cols);
    }

    /**
     * Shrinks the underlying table so that each column
     * is trimmed to its exact size.
     * @see #minimize
     */
    public void trim() {
        elements.trim();
        for(Int2DoubleOpenHashMap col : elements.values())
            col.trim();
    }

    /**
     * Shrinks the underlying table so that all values
     * {@link #findAllZeros equivalent to zero} are stripped.
     * This method is useful to significantly reduce memory
     * footprint after some kind of initialization step
     * fills this matrix with values. This method doesn't 
     * need to be called under normal circumstances since zeros 
     * are automatically discarded procedurally during 
     * modification operations {@link #add}, {@link #subtract},
     * {@link #operate}, and {@link #setValue}
     * @see #trim
     */
    public void minimize() {
        final List<int[]> zeros = findAllZeros();
        for(int[] coord : zeros) clearValue(coord[0], coord[1]);
        trim();
    }

    /**
     * Finds every value that is zero (or approxmiately zero such that 
     * <code>value < 0.00001d</code>) in this sparse matrix. Returns a 
     * list of coordinates describing the location of all these zeros
     * within this matrix.
     * @return A list of coordinates, where each memeber is a primitive 
     * integer array <code>[row, col]</code>
     */
    public List<int[]> findAllZeros() {
        List<int[]> output = new ArrayList<>(this.area());
        for(Int2ObjectMap.Entry<Int2DoubleOpenHashMap> row : elements.int2ObjectEntrySet()) {
            for(Int2DoubleMap.Entry col : row.getValue().int2DoubleEntrySet()) {
                if(isEquivalentToZero(col.getDoubleValue()))
                    output.add(new int[] { row.getIntKey(), col.getIntKey() });
            }
        }
        return output;
    }

    private boolean isEquivalentToZero(double value) {
        return Math.abs(value) <= EPSILON;
    }

    /**
     * @return The amount of rows in this matrix
     */
    public int rows() {
        return rows;
    }

    /**
     * @return The amount of columns in this matrix
     */
    public int cols() {
        return cols;
    }

    public boolean isSameSizeAs(SparseDoubleMatrix other) {
        return (this.rows != other.rows) || (this.cols != other.cols);
    }

    /**
     * The area of a sparse matrix is defined by its expected size 
     * when viewed as a table of rows an columns. 
     * @see {@link #footprint}
     * @return <code>rows() * cols()</code> of this matrix
     */
    public int area() { 
        return rows * cols;
    }

    /**
     * The actual size of this sparse matrix (not to be confused with its {@link #area})
     * serves as a loose descriptor of its memory footprint. This method computes the
     * length of each column in this matrix and returns it.
     * @see {@link #minimize} to trim this sparse matrix so that calls to this method return the smallest possible number
     * @return The total size of each column in this matrix
     */
    public int footprint() {
        int out = 0;
        for(Int2DoubleOpenHashMap col : elements.values())
            out += col.size();
        return out;
    }

    
    /**
     * Gets the contents of this matrix as a two-dimensional array where
     * the sparse population is filled. In other words, this method returns
     * a primitive dense matrix copy of this sparse matrix.
     * @see #asFlatArray
     * @return A new two-dimensional array of doubles.
     */
    public double[][] asTable() {
        double[][] out = new double[rows][cols];
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++)
                out[row][col] = get(row, col);
        }
        return out;
    }

    /**
     * Gets this matrix collapsed into one dimension where consecutive columns 
     * are strung together end-to-end. The resulting array will include
     * values that are equivalent to zero.
     * @see #asTable
     * @return A new array of doubles
     */
    public double[] asFlatArray() {
        double[] out = new double[area()];
        int iter = 0;
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                out[iter] = get(row, col);
                iter++;
            }
        }
        return out;
    }

    /**
     * Iterates over every value in this matrix and executes <code>cons</code>
     * for each one. 
     * @param cons TriConsumer formatted as <code>[row, column, value]</code>
     */
    public void forEachMemeber(TriConsumer<Integer, Integer, Double> cons) {
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                double value = get(row, col);
                cons.accept(row, col, value);
            }
        }
    }

    /**
     * Gets the contents of this matrix as a formatted
     * string with whitespace and line breaks. 
     * @return A single string spanning multiple lines
     */
    public String asString() {
        String out = "";
        for(int row = 0; row < rows; row++) {
            String rowContents = "";
            for(int col = 0; col < cols; col++) {
                double value = get(row, col);
                rowContents += String.format("%.3f", value) + ", ";
            }
            out += "\t[" + rowContents.substring(0, rowContents.length() - 2) + "]\n";
        }
        return "{\n" + out + "}";
    }

    public String toString() {
        return "SparseMatrix[" + rows + ", " + cols + "]";
    }

    private void assertInBounds(int row, int col) {
        if((row < 0 || row >= rows) || (col < 0 || col >= cols)) 
            throw new IndexOutOfBoundsException("The coordinate [" + row + ", " + col + "] is out of bounds for " + this);
    }

    private void assertSameSizeAs(SparseDoubleMatrix other) {
        if(!isSameSizeAs(other))
            throw new IllegalArgumentException(this + " does not share the same dimensions as " + other);
    }
}


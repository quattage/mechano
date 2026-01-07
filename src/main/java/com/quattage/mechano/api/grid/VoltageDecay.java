package com.quattage.mechano.api.grid;

/**
 * A library of LUTs for mapping an arbitrary (usually scalar)
 * value to open-circuit voltage. A VoltageDecay object can be 
 * stored by an energy producer/consumer to declare a voltage 
 * output in response to state of charge, thermal load, RPM, etc.
 */
public interface VoltageDecay {

    /**
     * A DataPoint represents a coorelation between an expected voltage
     * value and a state of charge %
     */
    public static record DataPoint(float soc, float volts) {
        public static DataPoint[] convert(float[][] table) {
            if(table.length < 2) throw new IllegalArgumentException("Failed to create VoltageDecay LUT - table must have at least 2 entries! (got " + table.length + ")");
            if(table.length > 255) throw new IllegalArgumentException("Failed to create VoltageDecay LUT - the provided table is larger than the maximum entry size of 255! (got " + table.length + ")");
            DataPoint[] output = new DataPoint[table.length];
            for(int x = 0; x < table.length; x++) {
                float[] entry = table[x];
                if(entry.length != 2) throw new IllegalArgumentException("Failed to create VoltageDecay LUT - entry at index " + x + " is of an unexpected size! (got " + entry.length + ", expected 2" + ")");
                output[x] = new DataPoint(entry[0], entry[1]);
            }
            return output;
        }
    }

    double apply(double soc);
    DataPoint[] getTable();
    double nominal();
    double minimal();
    double cutoff();
    default double clamp(double in) {
        return Math.clamp(in, nominal(), minimal());
    }

    /**
     * A single voltage value.
     * This implementation is used in place of a LUT or decay
     * function for a constant voltage response regardless of SoC
     */
    public static class Constant implements VoltageDecay {

        public float voltage;
        public Constant(float voltage) { this.voltage = voltage; }
        public void setVoltage(float voltage) { this.voltage = voltage; }
        @Override public double apply(double soc) { return voltage; }
        @Override public double nominal() { return voltage; }
        @Override public double minimal() { return voltage; }
        @Override public double cutoff() { return voltage - 0.1d; }

        @Override 
        public DataPoint[] getTable() { 
            return new DataPoint[] { 
                new DataPoint(0, voltage), new DataPoint(1, voltage)  
            };
        }

    }

    /**
     * A single sloped line.
     */
    public static class Linear implements VoltageDecay {
        
        private final float min;
        private final float max;
        public Linear(float min, float max) { this.min = min; this.max = max; }
        @Override public double apply(double soc) { return min + (max - min) * soc; }
        @Override public double nominal() { return max; }
        @Override public double minimal() { return min; }
        @Override public double cutoff() { return min - 0.1d; }

        @Override
        public DataPoint[] getTable() {
            return new DataPoint[] { 
                new DataPoint(0, min), new DataPoint(1, max)
            };
        }
    }

    /**
     * A standard LUT with no interpolation
     */
    public static class LUT implements VoltageDecay {

        protected final DataPoint[] table;

        public LUT(float[][] table) {
            this.table = DataPoint.convert(table);
        }

        public LUT(DataPoint[] points) {
            this.table = points;
        }

        @Override public double nominal() { return table[1].volts; }
        @Override public double minimal() { return table[0].volts; }
        @Override public double cutoff() { return minimal(); }

        @Override
        public double apply(double soc) {
            soc = Math.max(soc, 0);
            if(soc <= table[0].soc) return table[0].volts;
            if(soc >= table[table.length - 1].soc) return table[table.length - 1].volts;
            for(int x = 0; x < table.length - 1; x++) {
                DataPoint current = table[x];
                DataPoint next = table[x + 1];
                if(soc >= current.soc && soc <= next.soc) 
                    return processDatapoint(soc, x, current, next);
            }
            return table[table.length - 1].volts;
        }

        protected double processDatapoint(double lookup, int x, DataPoint current, DataPoint next) {
            return current.volts + ((next.volts - current.volts) * 0.5f);
        }

        @Override
        public DataPoint[] getTable() {
            return this.table;
        }
    }

    /**
     * A LUT with linear interpolation between points. When plotted, this LUT forms
     * a nice line graph with hard corners.
     */
    public static class LinearLUT extends LUT{

        protected static DataPoint[] convert(float[][] table) {
            if(table.length < 2) throw new IllegalArgumentException("Failed to create VoltageDecay LUT - table must have at least 2 entries! (got " + table.length + ")");
            if(table.length > 255) throw new IllegalArgumentException("Failed to create VoltageDecay LUT - the provided table is larger than the maximum entry size of 255! (got " + table.length + ")");
            DataPoint[] output = new DataPoint[table.length];
            for(int x = 0; x < table.length; x++) {
                float[] entry = table[x];
                if(entry.length != 2) throw new IllegalArgumentException("Failed to create VoltageDecay LUT - entry at index " + x + " is of an unexpected size! (got " + entry.length + ", expected 2" + ")");
                output[x] = new DataPoint(entry[0], entry[1]);
            }
            return output;
        }

        public LinearLUT(float[][] table) {
            super(LinearLUT.convert(table));
        }

        public LinearLUT(DataPoint[] points) {
            super(points);
        }

        @Override
        protected double processDatapoint(double lookup, int x, DataPoint current, DataPoint next) {
            double t = (lookup - current.soc) / (next.soc - current.soc);
            return current.volts + (next.volts - current.volts) * t; 
        }
    }


    /**
     * A LUT with a stairstepped cubic interpolation that creates
     * smooth transitions where each datapoint forms a ridge when graphed.
     * This is particularly useful creating sigmoid-like graphs.
     */
    public static class SteppedLUT extends LUT {

        public SteppedLUT(float[][] table) {
            super(table);
        }

        public SteppedLUT(DataPoint[] points) {
            super(points);
        }

        @Override
        protected double processDatapoint(double lookup, int x, DataPoint current, DataPoint next) {
            double t = ease((lookup - current.soc) / (next.soc - current.soc));
            return current.volts + (next.volts - current.volts) * t; 
        }

        private double ease(double x) {
            return x < 0.5f ? 4f * x * x * x : 1f - (float)Math.pow(-2f * x + 2f, 3f) / 2f;
        }
    }

    /**
     * A LUT using catmull-rom cubic spline interpolation to generate
     * a curve that conforms to the provided dataset.
     */
    public static class CubicLUT extends LUT {

        public CubicLUT(float[][] table) {
            super(table);
        }

        public CubicLUT(DataPoint[] points) {
            super(points);
        }

        @Override
        protected double processDatapoint(double lookup, int x, DataPoint current, DataPoint next) {
            DataPoint previous = (x > 0) ? table[x - 1] : current;
            DataPoint supernext = (x < table.length - 2) ? table[x + 2] : next;
            double t = Math.max(0f, Math.min(1f, (lookup - current.soc) / (next.soc - current.soc)));
            double t2 = t * t, t3 = t2 * t;
            // catmull–Rom spline interpolation
            return 0.5f * (
                (2f * current.volts) +
                (-previous.volts + next.volts) * t +
                (2f * previous.volts - 5f * current.volts + 4f * next.volts - supernext.volts) * t2 +
                (-previous.volts + 3f * current.volts - 3f * next.volts + supernext.volts) * t3
            );
        }
    }
}

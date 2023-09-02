package com.quattage.mechano.foundation.block.orientation.relative;

public class XY {

    public int x;
    public int y;

    public XY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public XY() {
        this.x = 0;
        this.y = 0;
    }

    public int x() { return x; }
    public int y() { return y; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public void setX() { this.x = 0; }
    public void setY() { this.y = 0; }

    public String toString() {
        return "[x: " + x + ", y: " + y + "]"; 
    }

    public boolean equals(Object other) {
        if(other instanceof XY c)
            return x == c.x() && y == c.y();
        return false;
    }

    public int hashCode() {
        return (this.x() + this.y() * 31) + this.x();
    }
}

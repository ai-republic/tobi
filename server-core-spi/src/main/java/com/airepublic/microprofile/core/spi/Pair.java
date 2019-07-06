package com.airepublic.microprofile.core.spi;

public class Pair<V1, V2> {
    private V1 value1;
    private V2 value2;


    public Pair() {
    }


    public Pair(final V1 value1, final V2 value2) {
        super();
        this.value1 = value1;
        this.value2 = value2;
    }


    public V1 getValue1() {
        return value1;
    }


    public void setValue1(final V1 value1) {
        this.value1 = value1;
    }


    public V2 getValue2() {
        return value2;
    }


    public void setValue2(final V2 value2) {
        this.value2 = value2;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (value1 == null ? 0 : value1.hashCode());
        result = prime * result + (value2 == null ? 0 : value2.hashCode());
        return result;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        if (value1 == null) {
            if (other.value1 != null) {
                return false;
            }
        } else if (!value1.equals(other.value1)) {
            return false;
        }
        if (value2 == null) {
            if (other.value2 != null) {
                return false;
            }
        } else if (!value2.equals(other.value2)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "Pair [value1=" + value1 + ", value2=" + value2 + "]";
    }

}

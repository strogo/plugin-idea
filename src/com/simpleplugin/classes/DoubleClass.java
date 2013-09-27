package com.simpleplugin.classes;

public class DoubleClass extends IntegralClass {

    int getWhole() {
        return 99999;
    }

    int getPrecision() {
        return 99999;
    }

    public boolean equals(Object obj) {
        return obj instanceof DoubleClass;
    }

    public int hashCode() {
        return 6;
    }

    @Override
    public String toString() {
        return "DOUBLE";
    }
}

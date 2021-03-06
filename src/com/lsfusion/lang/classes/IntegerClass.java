package com.lsfusion.lang.classes;

import com.lsfusion.util.BaseUtils;

import java.util.Collection;

public class IntegerClass extends IntClass {

    public final static IntegerClass instance = new IntegerClass();

    int getWhole() {
        return 10;
    }

    int getPrecision() {
        return 0;
    }

    public boolean equals(Object obj) {
        return obj instanceof IntegerClass;
    }

    public int hashCode() {
        return 7;
    }

    public String getName() {
        return "INTEGER";
    }

    @Override
    public Collection<String> getExtraNames() {
        return BaseUtils.toList("Result", "Quantity");
    }

    @Override
    public String getCaption() {
        return "Integer";
    }
}

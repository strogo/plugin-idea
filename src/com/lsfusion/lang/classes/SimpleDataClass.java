package com.lsfusion.lang.classes;

import java.util.Collection;
import java.util.Collections;

public class SimpleDataClass extends DataClass {

    private String dataName;

    public SimpleDataClass(String dataName) {
        this.dataName = dataName;
    }

    public DataClass op(DataClass compClass, boolean or, boolean string) {
        if (compClass.equals(this))
            return this;
        return null;
    }

    @Override
    public String getPreferredMask() {
        return "1234567";
    }

    public boolean equals(Object o) {
        return this == o || o instanceof SimpleDataClass && dataName.equals(((SimpleDataClass) o).dataName);
    }

    public int hashCode() {
        return dataName.hashCode();
    }

    public String getName() {
        return dataName;
    }

    public Collection<String> getExtraNames() {
        if(dataName.equals("DATETIME"))
            return Collections.singletonList("DateTime");
        return super.getExtraNames();
    }

    @Override
    public String getCaption() {
        return null;
    }
}

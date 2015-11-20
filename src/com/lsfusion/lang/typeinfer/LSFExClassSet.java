package com.lsfusion.lang.typeinfer;

import com.lsfusion.lang.classes.LSFClassSet;
import com.lsfusion.lang.classes.LogicalClass;
import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LSFExClassSet {
    public final LSFClassSet classSet;
    public final boolean orAny;

    public LSFExClassSet(LSFClassSet classSet) {
        this(classSet, false);
    }

    public static LSFExClassSet toEx(LSFClassSet set) {
        if(set == null) return null;
        return new LSFExClassSet(set);
    }
    
    public static LSFClassSet fromEx(LSFExClassSet set) {
        if(set == null) return null;
        return set.classSet;
    }
    
    public final static LSFExClassSet logical = new LSFExClassSet(LogicalClass.instance);

    public LSFExClassSet(LSFClassSet classSet, boolean orAny) {
        assert classSet!=null; 
        this.classSet = classSet;
        this.orAny = orAny;
    }

    public static <T> Map<T, LSFExClassSet> toEx(Map<T, LSFClassSet> classes) {
        Map<T, LSFExClassSet> result = new HashMap<T, LSFExClassSet>();
        for(Map.Entry<T, LSFClassSet> paramClass : classes.entrySet()) {
            result.put(paramClass.getKey(), new LSFExClassSet(paramClass.getValue(), false));
        }
        return result;
    }

    public static <T> Map<T, LSFClassSet> fromEx(Map<T, LSFExClassSet> classes) {
        Map<T, LSFClassSet> result = new HashMap<T, LSFClassSet>();
        for(Map.Entry<T, LSFExClassSet> paramClass : classes.entrySet()) {
            result.put(paramClass.getKey(), fromEx(paramClass.getValue()));
        }
        return result;
    }

    public static List<LSFExClassSet> toEx(List<LSFClassSet> classes) {
        if(classes == null)
            return null;
        
        List<LSFExClassSet> result = new ArrayList<LSFExClassSet>();
        for(LSFClassSet paramClass : classes) {
            result.add(toEx(paramClass));
        }
        return result;
    }
    
    public static List<LSFClassSet> fromEx(List<LSFExClassSet> classes) {
        if(classes == null)
            return null;
        
        List<LSFClassSet> result = new ArrayList<LSFClassSet>();
        for(LSFExClassSet paramClass : classes) {
            result.add(fromEx(paramClass));
        }
        return result;
    }

    public LSFExClassSet opNull(boolean or) {
        if(or)
            return new LSFExClassSet(classSet, true);
        return this;
    }

    // на конфликты типов (STRING - DATE например) по сути проверяет
    public static LSFExClassSet checkNull(LSFClassSet set, boolean orAny) {
        if(set == null)
            return null;
        
        return new LSFExClassSet(set, orAny);        
    }
    
    // может быть null
    @Nullable
    public LSFExClassSet op(LSFExClassSet exClassSet, boolean or) {
        return op(exClassSet, or, false);
    }
    @Nullable
    public LSFExClassSet op(LSFExClassSet exClassSet, boolean or, boolean string) {
        if(or) {
            return LSFExClassSet.checkNull(classSet.op(exClassSet.classSet, true, string), orAny || exClassSet.orAny);
        } else {
            assert !string;
            if(orAny) {
                if(exClassSet.orAny) // если оба orAny, то не and'м, а or'м
                    return LSFExClassSet.checkNull(classSet.op(exClassSet.classSet, true, string), true);
                return exClassSet;
            }
            if(exClassSet.orAny)
                return this;
            return LSFExClassSet.checkNull(classSet.op(exClassSet.classSet, false, string), false);
        }
    }
    
    public LSFExClassSet orAny() {
        return new LSFExClassSet(classSet, true);
    }

    public static LSFExClassSet orAny(LSFExClassSet set) {
        if(set == null) return set;
        return set.orAny();
    }
}

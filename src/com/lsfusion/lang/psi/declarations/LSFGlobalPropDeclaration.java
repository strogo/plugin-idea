package com.lsfusion.lang.psi.declarations;

import com.lsfusion.lang.psi.LSFDataPropertyDefinition;
import com.lsfusion.lang.psi.stubs.PropStubElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LSFGlobalPropDeclaration extends LSFFullNameDeclaration<LSFGlobalPropDeclaration, PropStubElement>, LSFPropDeclaration {

    String getCaption();

    boolean isAction();
    
    @Nullable
    LSFDataPropertyDefinition getDataPropertyDefinition();

    boolean isDataStoredProperty();

    public List<String> resolveParamNames();
}

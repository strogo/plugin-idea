package com.lsfusion.lang.psi.context;

import com.lsfusion.lang.psi.LSFClassParamDeclareList;
import com.lsfusion.lang.psi.LSFMappedPropertyClassParamDeclare;
import com.lsfusion.lang.psi.LSFPsiImplUtil;
import com.lsfusion.lang.psi.declarations.LSFExprParamDeclaration;
import com.lsfusion.lang.psi.declarations.LSFParamDeclaration;
import com.lsfusion.util.BaseUtils;

import java.util.List;
import java.util.Set;

public class ExplicitContextModifier implements ContextModifier {
    private final LSFClassParamDeclareList explicit;
    
    public ExplicitContextModifier(LSFMappedPropertyClassParamDeclare decl) {
        this(decl.getClassParamDeclareList());
    }
    
    public ExplicitContextModifier(LSFClassParamDeclareList explicit) {
        this.explicit = explicit;
    }

    @Override
    public List<LSFExprParamDeclaration> resolveParams(int offset, Set<LSFExprParamDeclaration> currentParams) {
        return BaseUtils.<LSFExprParamDeclaration, LSFParamDeclaration>immutableCast(LSFPsiImplUtil.resolveParams(explicit));
    }
}
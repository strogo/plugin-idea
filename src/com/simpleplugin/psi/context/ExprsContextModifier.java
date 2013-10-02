package com.simpleplugin.psi.context;

import com.intellij.psi.PsiElement;
import com.simpleplugin.psi.LSFPropertyExpression;

import java.util.Collections;
import java.util.List;

public class ExprsContextModifier extends ElementsContextModifier {
    
    private final List<LSFPropertyExpression> exprs;

    public ExprsContextModifier(LSFPropertyExpression expr) {
        this.exprs = Collections.singletonList(expr);
    }

    public ExprsContextModifier(List<LSFPropertyExpression> exprs) {
        this.exprs = exprs;
    }

    protected List<? extends PsiElement> getElements() {
        return exprs;
    }
}
package com.lsfusion.lang.psi;

import com.intellij.psi.PsiElement;

public interface LSFLocalizedStringValueLiteral extends PsiElement {
    String getValue();
    
    boolean needToBeLocalized();
}

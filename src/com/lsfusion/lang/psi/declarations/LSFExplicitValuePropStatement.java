package com.lsfusion.lang.psi.declarations;

import com.intellij.psi.StubBasedPsiElement;
import com.lsfusion.lang.psi.LSFPropertyStatement;
import com.lsfusion.lang.psi.stubs.interfaces.ExplicitValueStubElement;

public interface LSFExplicitValuePropStatement extends StubBasedPsiElement<ExplicitValueStubElement> {
    LSFPropertyStatement getPropertyStatement();

    LSFImplicitValuePropStatement getImplicitValuePropertyStatement();
}
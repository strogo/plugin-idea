package com.lsfusion.lang.psi.references.impl;

import com.intellij.lang.ASTNode;
import com.lsfusion.lang.classes.LSFClassSet;
import com.lsfusion.lang.psi.declarations.LSFGroupObjectDeclaration;
import com.lsfusion.lang.psi.extend.LSFFormExtend;
import com.lsfusion.lang.psi.references.LSFGroupObjectReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class LSFGroupObjectReferenceImpl extends LSFFormElementReferenceImpl<LSFGroupObjectDeclaration> implements LSFGroupObjectReference {
    protected LSFGroupObjectReferenceImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    protected FormExtendProcessor<LSFGroupObjectDeclaration> getElementsCollector() {
        return new FormExtendProcessor<LSFGroupObjectDeclaration>() {
            public Collection<LSFGroupObjectDeclaration> process(LSFFormExtend formExtend) {
                return formExtend.getGroupObjectDecls();
            }
        };
    }

    @Nullable
    public List<LSFClassSet> resolveClasses() {
        LSFGroupObjectDeclaration decl = resolveDecl();
        if(decl == null)
            return null;
        return decl.resolveClasses();
    }

}

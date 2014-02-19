package com.simpleplugin.psi.references.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.simpleplugin.LSFDeclarationResolveResult;
import com.simpleplugin.classes.LSFClassSet;
import com.simpleplugin.psi.declarations.LSFExprParamDeclaration;
import com.simpleplugin.psi.references.LSFAbstractParamReference;
import com.simpleplugin.psi.references.LSFObjectReference;
import com.simpleplugin.typeinfer.InferResult;
import com.simpleplugin.util.LSFPsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class LSFAbstractParamReferenceImpl<T extends LSFExprParamDeclaration> extends LSFReferenceImpl<T> implements LSFAbstractParamReference<T> {

    public LSFAbstractParamReferenceImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    protected PsiElement getParamDeclare() {
        return this;
    }

    private Set<LSFExprParamDeclaration> getContextParams() {
        PsiElement paramDecl = getParamDeclare();
        return LSFPsiUtils.getContextParams(paramDecl, this instanceof LSFObjectReference);
    }

    @Nullable
    @Override
    public LSFClassSet resolveClass() {
        T decl = resolveDecl();
        if(decl == null)
            return null;
        return decl.resolveClass();
    }

    @Nullable
    @Override
    public LSFClassSet resolveInferredClass(@Nullable InferResult inferred) {
        T decl = resolveDecl();
        if(decl == null)
            return null;
        LSFClassSet result = decl.resolveClass();
        if(result == null && inferred != null)
            result = inferred.get(decl);
        return result;
    }

    @Override
    public LSFDeclarationResolveResult resolveNoCache() {
        final List<T> objects = new ArrayList<T>();
        if (getSimpleName() != null) {
            final String nameRef = getNameRef();
            for (LSFExprParamDeclaration decl : getContextParams()) {
                if (decl.getDeclName().equals(nameRef)) {
                    objects.add((T) decl);
                }
            }
        }
        return new LSFDeclarationResolveResult(objects, resolveDefaultErrorAnnotator(objects));
    }
}

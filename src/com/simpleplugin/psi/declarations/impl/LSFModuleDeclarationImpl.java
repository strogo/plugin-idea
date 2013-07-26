package com.simpleplugin.psi.declarations.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.simpleplugin.BaseUtils;
import com.simpleplugin.psi.*;
import com.simpleplugin.psi.declarations.LSFModuleDeclaration;
import com.simpleplugin.psi.declarations.LSFNamespaceDeclaration;
import com.simpleplugin.psi.references.LSFModuleReference;
import com.simpleplugin.psi.references.LSFNamespaceReference;
import com.simpleplugin.psi.stubs.ModuleStubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// extends NamespaceDeclaration, при assertion'е что getNamespaceName==null
public abstract class LSFModuleDeclarationImpl extends LSFNamespaceDeclarationImpl<LSFModuleDeclaration, ModuleStubElement> implements LSFModuleDeclaration {

    protected LSFModuleDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    protected LSFModuleDeclarationImpl(@NotNull ModuleStubElement moduleStubElement, @NotNull IStubElementType nodeType) {
        super(moduleStubElement, nodeType);
    }

    @NotNull
    protected abstract LSFModuleName getModuleName();

    @Nullable
    protected abstract LSFRequireList getRequireList();

    @Nullable
    protected abstract LSFPriorityList getPriorityList();

    @Nullable
    protected abstract LSFNamespaceName getNamespaceName();

    @Override
    public LSFNamespaceDeclaration getNamespace() {
        LSFNamespaceName namespace = getNamespaceName();
        if(namespace==null)
            return this;
        return namespace.getNamespaceUsage().resolve();
    }

    @Override
    public List<LSFNamespaceReference> getPriorityRefs() {
        LSFPriorityList priorityList = getPriorityList();
        if(priorityList==null)
            return new ArrayList<LSFNamespaceReference>();
        return BaseUtils.<LSFNamespaceReference, LSFNamespaceUsage>immutableCast(priorityList.getNonEmptyNamespaceUsageList().getNamespaceUsageList());
    }

    @Override
    public List<LSFModuleReference> getRequireRefs() {
        LSFRequireList requireList = getRequireList();
        if(requireList==null)
            return new ArrayList<LSFModuleReference>();
        return BaseUtils.<LSFModuleReference, LSFModuleUsage>immutableCast(requireList.getNonEmptyModuleUsageList().getModuleUsageList());
    }

    @Override
    public LSFSimpleName getNameIdentifier() {
        return getModuleName().getSimpleName();
    }
}

package com.simpleplugin.psi.declarations.impl;

import com.intellij.icons.AllIcons;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.simpleplugin.psi.LSFId;
import com.simpleplugin.psi.LSFSimpleNameWithCaption;
import com.simpleplugin.psi.declarations.LSFGroupDeclaration;
import com.simpleplugin.psi.stubs.GroupStubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class LSFGroupDeclarationImpl extends LSFFullNameDeclarationImpl<LSFGroupDeclaration, GroupStubElement> implements LSFGroupDeclaration {

    public LSFGroupDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    public LSFGroupDeclarationImpl(@NotNull GroupStubElement groupStubElement, @NotNull IStubElementType nodeType) {
        super(groupStubElement, nodeType);
    }

    protected abstract LSFSimpleNameWithCaption getSimpleNameWithCaption();

    @Override
    public LSFId getNameIdentifier() {
        return getSimpleNameWithCaption().getSimpleName();
    }

    @Nullable
    @Override
    public Icon getIcon(int flags) {
        return AllIcons.Actions.GroupByModuleGroup;
    }
}

package com.lsfusion.lang.psi.declarations.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.lsfusion.LSFIcons;
import com.lsfusion.lang.classes.LSFValueClass;
import com.lsfusion.lang.psi.*;
import com.lsfusion.lang.psi.declarations.LSFTableDeclaration;
import com.lsfusion.lang.psi.stubs.TableStubElement;
import com.lsfusion.lang.psi.stubs.types.FullNameStubElementType;
import com.lsfusion.lang.psi.stubs.types.LSFStubElementTypes;
import com.lsfusion.refactoring.ElementMigration;
import com.lsfusion.refactoring.TableMigration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class LSFTableDeclarationImpl extends LSFFullNameDeclarationImpl<LSFTableDeclaration, TableStubElement> implements LSFTableDeclaration {

    public LSFTableDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    public LSFTableDeclarationImpl(@NotNull TableStubElement tableStubElement, @NotNull IStubElementType nodeType) {
        super(tableStubElement, nodeType);
    }

    @Nullable
    protected abstract LSFSimpleName getSimpleName();

    @Nullable
    @Override
    public LSFId getNameIdentifier() {
        return getSimpleName();
    }

    @Nullable
    @Override
    public Icon getIcon(int flags) {
        return LSFIcons.TABLE;
    }

    @Override
    protected FullNameStubElementType getType() {
        return LSFStubElementTypes.TABLE;
    }

    @Nullable
    public abstract LSFClassNameList getClassNameList();
    
    @NotNull
    public LSFValueClass[] getClasses() {
        LSFClassNameList classNameList = getClassNameList();
        if (classNameList == null)
            return new LSFValueClass[0];
        
        LSFNonEmptyClassNameList nonEmptyClassNameList = classNameList.getNonEmptyClassNameList();
        if (nonEmptyClassNameList == null) {
            return new LSFValueClass[0];
        }

        List<LSFClassName> list = nonEmptyClassNameList.getClassNameList();
        
        LSFValueClass[] classes = new LSFValueClass[list.size()];

        for (int i = 0; i < list.size(); i++) {
            classes[i] = LSFPsiImplUtil.resolveValueClass(list.get(i));
        }
        return classes;
    }
    
    @NotNull
    @Override
    public String[] getClassNames() {
        LSFClassNameList classNameList = getClassNameList();
        if (classNameList != null) {
            LSFNonEmptyClassNameList nonEmptyList = classNameList.getNonEmptyClassNameList();
            if (nonEmptyList != null) {
                List<LSFClassName> list = nonEmptyList.getClassNameList();

                String[] classNames = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    classNames[i] = LSFPsiImplUtil.getClassName(list.get(i));
                }

                return classNames;
            }
        }
        
        return new String[0];
    }
    
    @Nullable
    protected abstract LSFNoDefault getNoDefault();

    @Override
    public boolean isExplicit() {
        return getNoDefault() != null;
    }

    @Override
    public ElementMigration getMigration(String newName) {
        return new TableMigration(this, getName(), newName);
    }
}

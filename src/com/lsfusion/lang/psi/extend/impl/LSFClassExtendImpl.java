package com.lsfusion.lang.psi.extend.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.lsfusion.LSFIcons;
import com.lsfusion.lang.LSFElementGenerator;
import com.lsfusion.lang.psi.*;
import com.lsfusion.lang.psi.declarations.LSFClassDeclaration;
import com.lsfusion.lang.psi.declarations.LSFFullNameDeclaration;
import com.lsfusion.lang.psi.declarations.LSFStaticObjectDeclaration;
import com.lsfusion.lang.psi.extend.LSFClassExtend;
import com.lsfusion.lang.psi.stubs.extend.ExtendClassStubElement;
import com.lsfusion.lang.psi.stubs.types.FullNameStubElementType;
import com.lsfusion.lang.psi.stubs.types.LSFStubElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class LSFClassExtendImpl extends LSFExtendImpl<LSFClassExtend, ExtendClassStubElement> implements LSFClassExtend {

    public LSFClassExtendImpl(@NotNull ExtendClassStubElement extendClassStubElement, @NotNull IStubElementType nodeType) {
        super(extendClassStubElement, nodeType);
    }

    public LSFClassExtendImpl(@NotNull ASTNode node) {
        super(node);
    }

    public abstract LSFClassDecl getClassDecl();

    @Nullable
    protected abstract LSFClassParentsList getClassParentsList();

    public abstract LSFExtendingClassDeclaration getExtendingClassDeclaration();

    @Nullable
    protected abstract LSFStaticObjectDeclList getStaticObjectDeclList();

    @Override
    public String getGlobalName() {
        ExtendClassStubElement stub = getStub();
        if (stub != null)
            return stub.getGlobalName();

        LSFExtendingClassDeclaration extend = getExtendingClassDeclaration();
        if (extend != null)
            return extend.getCustomClassUsageWrapper().getCustomClassUsage().getNameRef();

        return getClassDecl().getGlobalName();
    }

    @Override
    public LSFFullNameDeclaration resolveDecl() {
        ExtendClassStubElement stub = getStub();
        if(stub != null) {
            return stub.getThis().resolveDecl(getLSFFile());
        }

        LSFClassDecl classDecl = getClassDecl();
        if (classDecl != null)
            return classDecl;

        return getExtendingClassDeclaration().getCustomClassUsageWrapper().getCustomClassUsage().resolveDecl();
    }

    @Nullable
    @Override
    protected LSFFullNameDeclaration getOwnDeclaration() {
        return getClassDecl();
    }

    @Nullable
    @Override
    protected LSFFullNameDeclaration resolveExtendingDeclaration() {
        LSFExtendingClassDeclaration extendingClassDeclaration = getExtendingClassDeclaration();
        if (extendingClassDeclaration != null) {
            return extendingClassDeclaration.getCustomClassUsageWrapper().getCustomClassUsage().resolveDecl();
        }
        return null;
    }

    @Override
    protected FullNameStubElementType<?, LSFClassDeclaration> getStubType() {
        return LSFStubElementTypes.CLASS;
    }

    @Override
    public List<LSFClassDeclaration> resolveExtends() {
        ExtendClassStubElement stub = getStub();
        if(stub != null) {
            return LSFStringClassRef.resolveDecls(stub.getExtends(), getLSFFile());
        }

        List<LSFClassDeclaration> result = new ArrayList<>();
        
        LSFClassParentsList parents = getClassParentsList();
        if (parents != null) {
            LSFNonEmptyCustomClassUsageList nonEmptyCustomClassUsageList = parents.getNonEmptyCustomClassUsageList();
            if (nonEmptyCustomClassUsageList != null) {
                for (LSFCustomClassUsage usage : nonEmptyCustomClassUsageList.getCustomClassUsageList()) {
                    LSFClassDeclaration decl = usage.resolveDecl();
                    if (decl != null) 
                        result.add(decl);
                }
            }
        }
        LSFStaticObjectDeclList staticObjectDeclList = getStaticObjectDeclList();
        if(staticObjectDeclList != null) {
            LSFNonEmptyStaticObjectDeclList nonEmptyStaticObjectDeclList = staticObjectDeclList.getNonEmptyStaticObjectDeclList();
            if(nonEmptyStaticObjectDeclList != null) {
                LSFClassDeclaration decl = LSFElementGenerator.getStaticObjectClassRef(getProject()).resolveDecl();
                if(decl != null)
                    result.add(decl);
            }
        }

        return result;
    }

    @Override
    public LSFStringClassRef getThis() {
        ExtendClassStubElement stub = getStub();
        if (stub != null)
            return stub.getThis();

        LSFClassDecl classDecl = getClassDecl();
        if (classDecl != null)
            return new LSFStringClassRef(null, false, classDecl.getSimpleNameWithCaption().getSimpleName().getText());

        return LSFPsiImplUtil.getClassNameRef(getExtendingClassDeclaration().getCustomClassUsageWrapper().getCustomClassUsage());
    }

    public List<LSFStringClassRef> getExtends() {
        ExtendClassStubElement stub = getStub();
        if (stub != null) 
            return stub.getExtends();

        List<LSFStringClassRef> result = new ArrayList<>();

        LSFClassParentsList parents = getClassParentsList();
        if (parents != null) {
            LSFNonEmptyCustomClassUsageList nonEmptyCustomClassUsageList = parents.getNonEmptyCustomClassUsageList();
            if (nonEmptyCustomClassUsageList != null) {
                for (LSFCustomClassUsage usage : nonEmptyCustomClassUsageList.getCustomClassUsageList()) {
                    result.add(LSFPsiImplUtil.getClassNameRef(usage));
                }
            }
        }

        LSFStaticObjectDeclList staticObjectDeclList = getStaticObjectDeclList();
        if(staticObjectDeclList != null) {
            LSFNonEmptyStaticObjectDeclList nonEmptyStaticObjectDeclList = staticObjectDeclList.getNonEmptyStaticObjectDeclList();
            if(nonEmptyStaticObjectDeclList != null)
                result.add(new LSFStringClassRef("System", "StaticObject"));
        }

        return result;
    }

    @Override
    public List<LSFStaticObjectDeclaration> getStaticObjects() {
        LSFStaticObjectDeclList listDecl = getStaticObjectDeclList();
        List<LSFStaticObjectDeclaration> result = new ArrayList<>();
        if (listDecl != null && listDecl.getNonEmptyStaticObjectDeclList() != null) {
            for (LSFStaticObjectDecl decl : listDecl.getNonEmptyStaticObjectDeclList().getStaticObjectDeclList()) {
                result.add(decl);
            }
        }
        return result;
    }

    public Set<LSFStaticObjectDeclaration> resolveStaticObjectDuplicates() {
        Set<LSFStaticObjectDeclaration> result = new HashSet<>();

        List<LSFStaticObjectDeclaration> localDecls = getStaticObjects();
        for (int i = 0; i < localDecls.size(); i++) {
            LSFStaticObjectDeclaration decl1 = localDecls.get(i);
            for (int j = 0; j < localDecls.size(); j++) {
                if (i != j) {
                    if (decl1.getNameIdentifier().getText().equals(localDecls.get(j).getNameIdentifier().getText())) {
                        result.add(decl1);
                        break;
                    }
                }
            }
        }

        List<LSFStaticObjectDeclaration> parentObjects = new ArrayList<>();
        for (LSFClassExtend extend : LSFGlobalResolver.findExtendElements(resolveDecl(), LSFStubElementTypes.EXTENDCLASS, getLSFFile()).findAll()) {
            if (!this.equals(extend)) {
                parentObjects.addAll(extend.getStaticObjects());
            }
        }

        for (LSFStaticObjectDeclaration decl : localDecls) {
            for (LSFStaticObjectDeclaration parentDecl : parentObjects) {
                if (decl.getNameIdentifier().getText().equals(parentDecl.getNameIdentifier().getText())) {
                    result.add(decl);
                    break;
                }
            }
        }
        return result;
    }

    @Nullable
    @Override
    public Icon getIcon(int flags) {
        return LSFIcons.CLASS;
    }
    
    public String getClassName() {
        LSFClassDecl decl = getClassDeclaration();
        return decl == null ? null : decl.getName();
    }
    
    public String getClassNamespace() {
        LSFClassDecl decl = getClassDeclaration();
        return decl == null ? null : decl.getNamespaceName();        
    }
    
    private LSFClassDecl getClassDeclaration() {
        LSFClassDecl decl = getClassDecl();
        if (decl == null) {
            LSFExtendingClassDeclaration extend = getExtendingClassDeclaration();
            if (extend != null) {
                LSFCustomClassUsage classUsage = extend.getCustomClassUsageWrapper().getCustomClassUsage();
                return (LSFClassDecl) classUsage.resolveDecl();
            }
        }
        return decl;
    }
}

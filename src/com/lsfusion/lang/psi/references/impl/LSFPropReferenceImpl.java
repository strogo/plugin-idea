package com.lsfusion.lang.psi.references.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CollectionQuery;
import com.lsfusion.lang.LSFElementGenerator;
import com.lsfusion.lang.LSFReferenceAnnotator;
import com.lsfusion.lang.classes.LSFClassSet;
import com.lsfusion.lang.meta.MetaTransaction;
import com.lsfusion.lang.psi.*;
import com.lsfusion.lang.psi.context.PropertyUsageContext;
import com.lsfusion.lang.psi.declarations.LSFDeclaration;
import com.lsfusion.lang.psi.declarations.LSFGlobalPropDeclaration;
import com.lsfusion.lang.psi.declarations.LSFLocalPropDeclaration;
import com.lsfusion.lang.psi.declarations.LSFPropDeclaration;
import com.lsfusion.lang.psi.references.LSFPropReference;
import com.lsfusion.lang.psi.stubs.types.FullNameStubElementType;
import com.lsfusion.lang.psi.stubs.types.LSFStubElementTypes;
import com.lsfusion.util.BaseUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class LSFPropReferenceImpl extends LSFActionOrPropReferenceImpl<LSFPropDeclaration, LSFGlobalPropDeclaration> implements LSFPropReference {

    public LSFPropReferenceImpl(@NotNull ASTNode node) {
        super(node);
    }

    protected FullNameStubElementType<?, LSFGlobalPropDeclaration> getStubElementType() {
        return LSFStubElementTypes.PROP;
    }

    public boolean isImplement() {
        PropertyUsageContext usageContext = getPropertyUsageContext();
        if(usageContext instanceof LSFMappedPropertyClassParamDeclare)
            return usageContext.getParent() instanceof LSFOverridePropertyStatement;
        return false;
    }

    @Override
    public PsiElement getWrapper() {
        return PsiTreeUtil.getParentOfType(this, LSFPropertyUsageWrapper.class);
    }

    private static class LocalResolveProcessor implements PsiScopeProcessor {

        private final String name;
        private Collection<LSFLocalPropDeclaration> found = new ArrayList<>();
        private final Condition<LSFPropDeclaration> condition;

        private LocalResolveProcessor(String name, Condition<LSFPropDeclaration> condition) {
            this.name = name;
            this.condition = condition;
        }

        @Override
        public boolean execute(@NotNull PsiElement element, ResolveState state) {
            if(element instanceof LSFLocalPropDeclaration) {
                LSFLocalPropDeclaration decl = (LSFLocalPropDeclaration) element;
                String declName = decl.getName();
                if (declName != null && declName.equals(this.name) && condition.value(decl)) {
                    found.add(decl);
                }
            }
            return true;
        }

        @Nullable
        @Override
        public <T> T getHint(@NotNull Key<T> hintKey) {
            return null;
        }

        @Override
        public void handleEvent(Event event, @Nullable Object associated) {
        }
    }

    @Override
    protected Collection<? extends LSFPropDeclaration> resolveDeclarations() {
        Collection<? extends LSFPropDeclaration> declarations = BaseUtils.emptyList();

        if (getFullNameRef() == null)
            declarations = resolveLocals(BaseUtils.immutableCast(getCondition()), getFinalizer());

        if (declarations.isEmpty()) {
            declarations = super.resolveDeclarations();
        }

        if(canBeUsedInDirect()) {
            if(declarations.isEmpty())
                declarations = resolveLocals(getInDirectCondition(), Finalizer.EMPTY);

            if(declarations.isEmpty())
                declarations = LSFGlobalResolver.findElements(getNameRef(), getFullNameRef(), getStubElementTypes(), getLSFFile(), BaseUtils.<Condition<LSFGlobalPropDeclaration>>immutableCast(getInDirectCondition()), Finalizer.EMPTY);
        }
        
        return declarations;
    }

    @Override
    protected Collection<? extends LSFPropDeclaration> resolveNoConditionDeclarations() {
        Collection<? extends LSFPropDeclaration> declarations = BaseUtils.emptyList();

        final List<LSFClassSet> usageClasses = getUsageContext();
        if (usageClasses != null) {
            Finalizer<LSFPropDeclaration> noConditionFinalizer = getNoConditionFinalizer(usageClasses);

            if (getFullNameRef() == null)
                declarations = resolveLocals(Condition.TRUE, BaseUtils.immutableCast(noConditionFinalizer));
            
            if(declarations.isEmpty())
                declarations = new CollectionQuery<LSFPropDeclaration>(LSFGlobalResolver.findElements(getNameRef(), getFullNameRef(), getStubElementTypes(), getLSFFile(), Condition.TRUE, BaseUtils.<Finalizer>immutableCast(noConditionFinalizer))).findAll();
        }
        return declarations;
    }

    private Collection<? extends LSFPropDeclaration> resolveLocals(Condition<LSFPropDeclaration> condition, Finalizer<LSFGlobalPropDeclaration> finalizer) {
        LocalResolveProcessor processor = new LocalResolveProcessor(getNameRef(), BaseUtils.<Condition<LSFPropDeclaration>>immutableCast(condition));
        PsiTreeUtil.treeWalkUp(processor, this, null, new ResolveState());
        Finalizer<LSFLocalPropDeclaration> castFinalizer = BaseUtils.immutableCast(finalizer);
        return castFinalizer.finalize(processor.found);
    }

    public boolean isNoContext(PropertyUsageContext usageContext) {
        return usageContext instanceof LSFNoContextPropertyUsage || usageContext instanceof LSFNoContextActionOrPropertyUsage;
    }

    @Override
    protected String getErrorName() {
        return "property";
    }
}

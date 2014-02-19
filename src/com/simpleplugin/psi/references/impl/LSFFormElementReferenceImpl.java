package com.simpleplugin.psi.references.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.util.EmptyQuery;
import com.intellij.util.Query;
import com.simpleplugin.LSFDeclarationResolveResult;
import com.simpleplugin.psi.LSFFile;
import com.simpleplugin.psi.LSFFormStatement;
import com.simpleplugin.psi.LSFGlobalResolver;
import com.simpleplugin.psi.context.FormContext;
import com.simpleplugin.psi.declarations.LSFDeclaration;
import com.simpleplugin.psi.declarations.LSFFormDeclaration;
import com.simpleplugin.psi.extend.LSFFormExtend;
import com.simpleplugin.psi.references.LSFFormElementReference;
import com.simpleplugin.psi.stubs.types.LSFStubElementTypes;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class LSFFormElementReferenceImpl<T extends LSFDeclaration> extends LSFReferenceImpl<T> implements LSFFormElementReference<T> {

    protected LSFFormElementReferenceImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    @Override
    public LSFDeclarationResolveResult resolveNoCache() {
        final List<T> objects = new ArrayList<T>();
        if (getSimpleName() != null) {
            Condition<T> filter = getResolvedDeclarationsFilter();
            for (T decl : collectElementsFromContext()) {
                if (filter.value(decl)) {
                    objects.add(decl);
                }
            }
        }
        return new LSFDeclarationResolveResult(objects, resolveDefaultErrorAnnotator(objects));
    }

    protected Condition<T> getResolvedDeclarationsFilter() {
        final String nameRef = getNameRef();
        return new Condition<T>() {
            public boolean value(T decl) {
                return decl.getDeclName().equals(nameRef);
            }
        };
    }

    private Set<T> collectElementsFromContext() {
        return processFormContext(this, getTextOffset(), getElementsCollector());
    }

    protected abstract FormExtendProcessor<T> getElementsCollector();

    public static interface FormExtendProcessor<T> {
        Collection<T> process(LSFFormExtend formExtend);
    }

    public static <T> Set<T> processFormContext(PsiElement current, int offset, final FormExtendProcessor<T> processor) {
        Set<T> processedContext = processFormContext(current, processor, true);
        if (processedContext != null) {
            return processedContext;
        }

        PsiElement parent = current.getParent();
        if (!(parent == null || parent instanceof LSFFile)) {
            return processFormContext(parent, offset, processor); // бежим выше
        }

        return new HashSet<T>();
    }

    public static <T> Set<T> processFormContext(PsiElement current, final FormExtendProcessor<T> processor, boolean objectRef) {
        Query<LSFFormExtend> extendForms = null;
        if (current instanceof FormContext && (objectRef || current instanceof LSFFormStatement)) {
            LSFFormDeclaration formDecl = ((FormContext) current).resolveFormDecl();
            extendForms = formDecl == null
                          ? new EmptyQuery<LSFFormExtend>()
                          : LSFGlobalResolver.findExtendElements(formDecl, LSFStubElementTypes.EXTENDFORM, (LSFFile) current.getContainingFile());
        }

        if (extendForms != null) {
            final Set<T> finalResult = new HashSet<T>();
            extendForms.forEach(new com.intellij.util.Processor<LSFFormExtend>() {
                public boolean process(LSFFormExtend formExtend) {
                    finalResult.addAll(processor.process(formExtend));
                    return true;
                }
            });
            return finalResult;
        }
        return null;
    }
}

package com.lsfusion.lang.psi.declarations;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.lsfusion.lang.meta.MetaTransaction;
import com.lsfusion.lang.psi.LSFElement;
import com.lsfusion.lang.psi.LSFId;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LSFDeclaration extends LSFElement, PsiNameIdentifierOwner, ItemPresentation {

    String getDeclName();

    @Nullable
    @Override
    LSFId getNameIdentifier();

    boolean resolveDuplicates();

    PsiElement[] processImplementationsSearch();

    void setName(@NonNls @NotNull String name, MetaTransaction transaction);
}

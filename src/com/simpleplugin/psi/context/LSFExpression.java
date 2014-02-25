package com.simpleplugin.psi.context;

import com.intellij.psi.PsiElement;
import com.simpleplugin.classes.LSFClassSet;
import com.simpleplugin.typeinfer.InferResult;
import com.simpleplugin.typeinfer.Inferred;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LSFExpression extends PsiElement {

    LSFClassSet resolveInferredValueClass(@Nullable InferResult inferred);

    Inferred inferParamClasses(@Nullable LSFClassSet valueClass);

    List<String> getValueClassNames();

    List<String> getValuePropertyNames();
}
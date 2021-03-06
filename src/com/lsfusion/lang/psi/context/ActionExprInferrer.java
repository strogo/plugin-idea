package com.lsfusion.lang.psi.context;

import com.lsfusion.lang.psi.declarations.LSFExprParamDeclaration;
import com.lsfusion.lang.typeinfer.Inferred;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ActionExprInferrer implements ContextInferrer {
    private final ActionExpression expr;

    public ActionExprInferrer(@NotNull ActionExpression expr) {
        this.expr = expr;
    }

    @Override
    public Inferred inferClasses(Set<LSFExprParamDeclaration> params) {
        return expr.inferActionParamClasses(params);
    }
}

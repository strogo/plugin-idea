package com.lsfusion.lang.renaming;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.lsfusion.lang.meta.MetaTransaction;
import com.lsfusion.lang.psi.LSFFile;

import java.util.Collections;

public class FileUnqualifyRefsAction extends AnAction {

    protected LSFFile getLSFFile(AnActionEvent e) {
        Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());
        if(editor != null) {
            PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, e.getProject());
            if(psiFile instanceof LSFFile)
                return (LSFFile) psiFile;
        }
        return null;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final LSFFile file = getLSFFile(e);
        if(file!=null) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    MetaTransaction transaction = new MetaTransaction();
                    LSFRenameFullNameProcessor.unqualifyRefs(Collections.singleton(file), transaction);
                    transaction.apply();
                }
            });
        }
    }
}
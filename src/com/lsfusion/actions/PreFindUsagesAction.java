package com.lsfusion.actions;

import com.intellij.find.actions.FindUsagesAction;
import com.intellij.openapi.actionSystem.AnAction;

public class PreFindUsagesAction extends UsagesSearchAction {
    @Override
    protected AnAction getPlatformAction() {
        return new FindUsagesAction();
    }
}

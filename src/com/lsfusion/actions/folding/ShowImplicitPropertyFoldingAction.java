package com.lsfusion.actions.folding;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

public class ShowImplicitPropertyFoldingAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent e) {
        return PropertyFoldingManager.isImplicit(getEventProject(e));
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Project project = getEventProject(e);
        if (project != null && !PropertyFoldingManager.isImplicit(project)) {
            PropertyFoldingManager.setImplicit(project);
            PropertyFoldingManager.refreshEditor(project);
        }
    }
}

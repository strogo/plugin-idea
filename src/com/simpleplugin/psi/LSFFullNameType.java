package com.simpleplugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.simpleplugin.psi.declarations.LSFGlobalDeclaration;

public interface LSFFullNameType<T extends LSFGlobalDeclaration> {

    StringStubIndexExtension<T> getIndex(Project project);
}

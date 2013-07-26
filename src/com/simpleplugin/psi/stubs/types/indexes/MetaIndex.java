package com.simpleplugin.psi.stubs.types.indexes;

import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import com.simpleplugin.psi.declarations.LSFMetaDeclaration;
import com.simpleplugin.psi.stubs.types.LSFStubElementTypes;
import org.jetbrains.annotations.NotNull;

public class MetaIndex extends StringStubIndexExtension<LSFMetaDeclaration> {

    private static final MetaIndex ourInstance = new MetaIndex();
    public static MetaIndex getInstance() {
        return ourInstance;
    }

    @NotNull
    @Override
    public StubIndexKey<String, LSFMetaDeclaration> getKey() {
        return LSFStubElementTypes.META.key;
    }
}

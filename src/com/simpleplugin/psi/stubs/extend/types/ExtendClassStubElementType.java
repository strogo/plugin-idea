package com.simpleplugin.psi.stubs.extend.types;

import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.simpleplugin.psi.extend.LSFClassExtend;
import com.simpleplugin.psi.extend.LSFFormExtend;
import com.simpleplugin.psi.impl.LSFClassStatementImpl;
import com.simpleplugin.psi.stubs.extend.ExtendClassStubElement;
import com.simpleplugin.psi.stubs.extend.ExtendFormStubElement;
import com.simpleplugin.psi.stubs.extend.impl.ExtendClassStubImpl;
import com.simpleplugin.psi.stubs.extend.types.indexes.ExtendClassIndex;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ExtendClassStubElementType extends ExtendStubElementType<LSFClassExtend, ExtendClassStubElement> {

    public ExtendClassStubElementType() {
        super("EXTEND_CLASS");
    }

    public StringStubIndexExtension<LSFClassExtend> getGlobalIndex() {
        return ExtendClassIndex.getInstance();
    }

    @Override
    public LSFClassExtend createPsi(@NotNull ExtendClassStubElement stub) {
        return new LSFClassStatementImpl(stub, this);
    }

    @Override
    public ExtendClassStubElement createStub(@NotNull LSFClassExtend psi, StubElement parentStub) {
        return new ExtendClassStubImpl(parentStub, psi);
    }

    @Override
    public ExtendClassStubElement deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new ExtendClassStubImpl(dataStream, parentStub, this);
    }
}

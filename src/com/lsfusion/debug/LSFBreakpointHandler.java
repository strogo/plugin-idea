package com.lsfusion.debug;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaBreakpointHandler;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.lsfusion.lang.psi.LSFFile;
import lsfusion.server.logics.debug.DebuggerService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LSFBreakpointHandler extends JavaBreakpointHandler {
    private List<Runnable> remoteEvents = new ArrayList<Runnable>();
    private DebuggerService debuggerService;
    
    public LSFBreakpointHandler(Class<? extends XBreakpointType<?, ?>> breakpointTypeClass, DebugProcessImpl process) {
        super(breakpointTypeClass, process);
    }
    
    private void reattachToService() {
        final Timer timerTask = new Timer();
        timerTask.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Registry registry = LocateRegistry.getRegistry(1299);
                    debuggerService = (DebuggerService) registry.lookup("lsfDebuggerService");
                    if (debuggerService != null) {
                        executePendingMethods();
                        timerTask.cancel();
                    }
                } catch (Throwable ignored) {
                }
            }
        }, 0, 1000);
    }
    
    private void executePendingMethods() {
        for (Runnable remoteEvent : remoteEvents) {
            remoteEvent.run();
        }
    }

    @Nullable
    @Override
    protected Breakpoint createJavaBreakpoint(@NotNull XBreakpoint xBreakpoint) {
        return super.createJavaBreakpoint(xBreakpoint);
    }
    
    @Override
    public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
        if (myProcess.getXdebugProcess() instanceof LSFDebugProcess) {
            executeMethod(true, breakpoint);

            super.registerBreakpoint(breakpoint);
        }
    }

    @Override
    public void unregisterBreakpoint(@NotNull XBreakpoint breakpoint, boolean temporary) {
        if (myProcess.getXdebugProcess() instanceof LSFDebugProcess) {
            executeMethod(false, breakpoint);

            super.unregisterBreakpoint(breakpoint, temporary);
        }
    }

    private String getModuleName(XBreakpoint breakpoint) {
        XSourcePosition position = breakpoint.getSourcePosition();
        if (position != null) {
            PsiFileSystemItem systemItem = FileReferenceHelper.getPsiFileSystemItem(PsiManager.getInstance(myProcess.getProject()), position.getFile());
            if (systemItem instanceof LSFFile) {
                return ((LSFFile) systemItem).getModuleDeclaration().getNameIdentifier().getName();
            }
        }
        return null;
    }
    
    private void executeMethod(boolean register, XBreakpoint breakpoint) {
        XSourcePosition position = breakpoint.getSourcePosition();
        String module = getModuleName(breakpoint);
        if (position == null || module == null) {
            return;
        }
        Integer line = position.getLine();
        
        if (myProcess.isDetached()) {
            scheduleRemoteInvocation(register, module, line);
        } else if (debuggerService == null) {
            scheduleRemoteInvocation(register, module, line);
            reattachToService();
        } else {
            invokeRemoteMethod(register, module, line);
        }
    }
    
    private void scheduleRemoteInvocation(final boolean register, final String module, final Integer line) {
        remoteEvents.add(new Runnable() {
            @Override
            public void run() {
                invokeRemoteMethod(register, module, line);
            }
        });    
    }
    
    public void invokeRemoteMethod(final boolean register, final String module, final Integer line) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (register) {
                        debuggerService.registerBreakpoint(module, line);
                    } else {
                        debuggerService.unregisterBreakpoint(module, line);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }
}

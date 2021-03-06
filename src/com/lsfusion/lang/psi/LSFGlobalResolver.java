package com.lsfusion.lang.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.lsfusion.lang.LSFElementGenerator;
import com.lsfusion.lang.psi.cache.RequireModulesCache;
import com.lsfusion.lang.psi.declarations.*;
import com.lsfusion.lang.psi.declarations.impl.LSFFormExtendElement;
import com.lsfusion.lang.psi.extend.LSFClassExtend;
import com.lsfusion.lang.psi.extend.LSFExtend;
import com.lsfusion.lang.psi.indexes.ClassExtendsClassIndex;
import com.lsfusion.lang.psi.references.LSFFullNameReference;
import com.lsfusion.lang.psi.references.LSFNamespaceReference;
import com.lsfusion.lang.psi.stubs.FullNameStubElement;
import com.lsfusion.lang.psi.stubs.extend.ExtendStubElement;
import com.lsfusion.lang.psi.stubs.extend.types.ExtendStubElementType;
import com.lsfusion.lang.psi.stubs.types.FullNameStubElementType;
import com.lsfusion.lang.psi.stubs.types.LSFStubElementTypes;
import com.lsfusion.util.BaseUtils;
import com.lsfusion.util.LSFPsiUtils;

import java.util.*;

public class LSFGlobalResolver {

    // тут можно использовать или RequireModulesCache (но проблема в том, что тогда он будет очищаться каждый раз и заново будет делать resolveDecl, с другой стороны там все за stub'но, так что не сильно большой оверхед)
    // или ручной кэш, он очищается только при изменении структуры модулей
    // пока попробуем автоматический
    public static Set<VirtualFile> getRequireModules(LSFModuleDeclaration declaration) {
//        return getCachedRequireModules(declaration);
        return getManualCachedRequireModules(declaration, new HashSet<VirtualFile>());
    }
    private static Set<VirtualFile> getCachedRequireModules(LSFModuleDeclaration declaration) {
        String name = declaration.getGlobalName();
        boolean toCache = name != null && !name.equals(LSFElementGenerator.genName) && !LSFPsiUtils.isInjected(declaration);
        if (toCache) {
            return RequireModulesCache.getInstance(declaration.getProject()).getRequireModulesWithCaching(declaration);
        }
        return getRequireModulesNoCache(declaration);
    }

    public static Set<VirtualFile> getRequireModulesNoCache(LSFModuleDeclaration declaration) {
        Set<VirtualFile> result = new HashSet<>();
        result.add(declaration.getLSFFile().getVirtualFile());
        for(LSFModuleDeclaration decl : declaration.getRequireModules())
            if(decl != null)
                result.addAll(getCachedRequireModules(decl));
        return result;
    }

    public static Map<LSFModuleDeclaration, Set<VirtualFile>> cached = ContainerUtil.createConcurrentWeakKeySoftValueMap();

    private static Set<VirtualFile> getManualCachedRequireModules(LSFModuleDeclaration declaration, Set<VirtualFile> alreadyGet) {
        String name = declaration.getGlobalName();
        boolean toCache = name != null && !name.equals(LSFElementGenerator.genName) && !LSFPsiUtils.isInjected(declaration);
        if (toCache) {
            Set<VirtualFile> cachedFiles = cached.get(declaration);
            if (cachedFiles != null)
                return cachedFiles;
        }

        Set<VirtualFile> result = new HashSet<>();

        VirtualFile declarationFile = declaration.getLSFFile().getVirtualFile();
        if (!alreadyGet.contains(declarationFile)) {
            result.add(declarationFile);
            alreadyGet.add(declarationFile);
            for (LSFModuleDeclaration decl : declaration.getRequireModules()) {
                Set<VirtualFile> requireModules = getManualCachedRequireModules(decl, alreadyGet);

                result.addAll(requireModules);
            }
//        System.out.println("CACHED "+declaration.getName()+" "+System.identityHashCode(declaration));
            if (toCache)
                cached.put(declaration, result);
        }
        return result;
    }

    public static GlobalSearchScope getRequireScope(LSFElement lsfElement) {
        return getRequireScope(lsfElement.getLSFFile());
    }

    public static GlobalSearchScope getRequireScope(LSFFile lsfFile) {
        if (lsfFile instanceof LSFCodeFragment && lsfFile.getContext() != null) {
            PsiFile containingFile = lsfFile.getContext().getContainingFile();
            if (containingFile instanceof LSFFile && containingFile != lsfFile) {
                return getRequireScope((LSFFile)containingFile);
            }
        }

        Project project = lsfFile.getProject();
        LSFModuleDeclaration declaration = lsfFile.getModuleDeclaration();
        VirtualFile vfile = lsfFile.getVirtualFile();
        if (vfile == null && declaration != null) {
            Query<LSFModuleDeclaration> modules = findModules(declaration.getGlobalName(), GlobalSearchScope.allScope(project));
            LSFModuleDeclaration first = modules.findFirst();
            if (first != null)
                declaration = first;
        }

        Set<VirtualFile> vFiles = new HashSet<>();
        if (declaration != null) {
            for (VirtualFile f : getRequireModules(declaration)) {
                if (f != null) {
                    vFiles.add(f); // null может быть только для dumb
                }
            }
        }
        return GlobalSearchScope.filesScope(project, vFiles);
    }

    private static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findInNamespace(Collection<T> decls) {
        return decls == null || decls.isEmpty() ? null : decls;
    }
    public static <S extends FullNameStubElement, T extends LSFFullNameDeclaration, SC extends FullNameStubElement<SC, TC>, TC extends LSFFullNameDeclaration<TC, SC>> Collection<T> findElements(String name, String fqName, Collection<FullNameStubElementType> types, LSFFile file, Integer offset, Condition<T> condition, Finalizer<T> finalizer) {
        Condition<TC> conditionC = BaseUtils.immutableCast(condition); Finalizer<TC> finalizerC = BaseUtils.immutableCast(finalizer);
        return BaseUtils.<Collection<T>>immutableCast(LSFGlobalResolver.<SC, TC>findElements(name, fqName, types, file, offset, conditionC, finalizerC, new ArrayList<TC>()));
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, String fqName, Collection<FullNameStubElementType> types, LSFFile file, Integer offset, Condition<T> condition, Finalizer<T> finalizer, List<T> virtDecls) {
        Collection<FullNameStubElementType<S, T>> fullNameStubElementTypes = BaseUtils.<Collection<FullNameStubElementType<S, T>>>immutableCast(types);
        return findElements(name, fqName, file, offset, fullNameStubElementTypes, condition, finalizer, virtDecls);
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, final String fqName, LSFFile file, Integer offset, Collection<? extends FullNameStubElementType<S, T>> types, Condition<T> condition) {
        return findElements(name, fqName, file, offset, types, condition, new ArrayList<>());
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, final String fqName, LSFFile file, Integer offset, Collection<? extends FullNameStubElementType<S, T>> types, Condition<T> condition, List<T> virtDecls) {
        return findElements(name, fqName, file, offset, types, condition, Finalizer.EMPTY, virtDecls);
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, final String fqName, LSFFile file, Integer offset, Collection<? extends FullNameStubElementType<S, T>> types, Condition<T> condition, Finalizer<T> finalizer, List<T> virtDecls) {
        if (fqName != null) {
            final Condition<T> fCondition = condition;
            condition = new Condition<T>() {
                public boolean value(T t) {
                    String namespace = t.getNamespaceName();
                    return namespace.equals(fqName) && fCondition.value(t);
                }
            };
        } else {
            LSFModuleDeclaration moduleDeclaration = file.getModuleDeclaration();
            if(moduleDeclaration != null)
                finalizer = new NamespaceFinalizer<>(moduleDeclaration, finalizer);
        }
        return findElements(name, file, offset, types, condition, finalizer, virtDecls);
    }

    private static class NamespaceFinalizer<T extends LSFFullNameDeclaration> implements Finalizer<T> {

        private final LSFModuleDeclaration moduleDeclaration;
        private final Finalizer<T> finalizer;

        private NamespaceFinalizer(LSFModuleDeclaration moduleDeclaration, Finalizer<T> finalizer) {
            this.moduleDeclaration = moduleDeclaration;
            this.finalizer = finalizer;
        }

        public Collection<T> finalize(Collection<T> decls) {
            if (decls.size() == 1) // оптимизация
                return decls;

            Map<String, Collection<T>> mapDecls = new HashMap<>();
            for (T decl : decls) {
                String namespace = decl.getNamespaceName();

                Collection<T> nameList = mapDecls.get(namespace);
                if (nameList == null) {
                    nameList = new ArrayList<>();
                    mapDecls.put(namespace, nameList);
                }
                nameList.add(decl);
            }

            List<String> fullPrioritiesList = new ArrayList<>();
            fullPrioritiesList.add(moduleDeclaration.getNamespace());
            for (LSFNamespaceReference priority : moduleDeclaration.getPriorityRefs())
                fullPrioritiesList.add(priority.getNameRef());
                
            // смотрим на priority
            //noinspection RedundantTypeArguments - отказывается компилироваться с language level 8
            for (String priority : fullPrioritiesList) {
                //noinspection RedundantTypeArguments - отказывается компилироваться с language level 8
                Collection<T> priorityDecls = mapDecls.get(priority);
                if (priorityDecls != null && !priorityDecls.isEmpty()) {
                    decls = priorityDecls;
                    break;
                }
            }
            return finalizer.finalize(decls);
        }
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, LSFFile file, Integer offset, Collection<? extends FullNameStubElementType<S, T>> types, Condition<T> condition, Finalizer<T> finalizer) {
        return findElements(name, file, offset, types, condition, finalizer, new ArrayList<>());
    }
    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, LSFFile file, Integer offset, Collection<? extends FullNameStubElementType<S, T>> types, Condition<T> condition, Finalizer<T> finalizer, List<T> virtDecls) {

        GlobalSearchScope scope = file.getRequireScope();

        Collection<T> decls = new ArrayList<>();
        Set<StringStubIndexExtension> usedIndices = new HashSet<>();
        for (FullNameStubElementType<S, T> type : types) {
            StringStubIndexExtension<T> index = type.getGlobalIndex();
            if(usedIndices.add(index))
                decls.addAll(index.get(name, file.getProject(), scope));
        }
        for (T virtDecl : virtDecls) {
            if (virtDecl != null && name != null && name.equals(virtDecl.getDeclName())) {
                VirtualFile virtualFile = virtDecl.getLSFFile().getVirtualFile();
                if (virtualFile == null || scope.contains(virtualFile)) {
                    decls.add(virtDecl);
                }
            }
        }

        Collection<T> fitDecls = new ArrayList<>();
        for (T decl : decls)
            if (condition.value(decl))
                if (offset == null || !isAfter(file, offset, decl))
                    fitDecls.add(decl);

        return finalizer.finalize(fitDecls);
    }
    
    // classes now are parsed first
    public static boolean isAfter(LSFFile file, int offset, LSFFullNameDeclaration decl) {
        return !(decl instanceof LSFClassDeclaration) && file == decl.getLSFFile() && decl.getOffset() > offset;
    }
    public static boolean isAfter(int offset, LSFFormExtendElement decl) { // should be also stubbed later
        return decl.getOffset() > offset; // later stubs should be added here together with all get*Decls
    }

    public static Query<LSFModuleDeclaration> findModules(String name, GlobalSearchScope scope) {
        StringStubIndexExtension<LSFModuleDeclaration> index = LSFStubElementTypes.MODULE.getGlobalIndex();
        Collection<LSFModuleDeclaration> declarations = index.get(name, scope.getProject(), scope);
        return new CollectionQuery<>(declarations);
    }

    public static Query<LSFNamespaceDeclaration> findNamespaces(String name, GlobalSearchScope scope) {

        Query<LSFNamespaceDeclaration> modules = BaseUtils.immutableCast(findModules(name, scope));
        if (modules.findFirst() != null)
            return modules;

        // модуля нет, ищем namespace'ы
        StringStubIndexExtension<LSFExplicitNamespaceDeclaration> explicitIndex = LSFStubElementTypes.EXPLICIT_NAMESPACE.getGlobalIndex();
        Collection<LSFExplicitNamespaceDeclaration> explicitDeclarations = explicitIndex.get(name, scope.getProject(), scope);
        if (explicitDeclarations.size() == 0)
            return modules;

        LSFExplicitNamespaceDeclaration minDeclaration = null;
        String minName = null;
        for (LSFExplicitNamespaceDeclaration explicitDecl : explicitDeclarations) {
            String moduleName = explicitDecl.getLSFFile().getModuleDeclaration().getDeclName();
            if (minName == null || minName.compareTo(moduleName) > 0) {
                minDeclaration = explicitDecl;
                minName = moduleName;
            }
        }
        return new MergeQuery<>(modules, new CollectionQuery<>(Collections.<LSFNamespaceDeclaration>singleton(minDeclaration)));
    }

    // этот элемент и все "выше"
    public static <E extends ExtendStubElement<T, E>, T extends LSFExtend<T, E>> Query<T> findExtendElements(T element) {
        return findExtendElements(element.resolveDecl(), (ExtendStubElementType<T, E>) element.getElementType(), element.getLSFFile());
    }

    public static <E extends ExtendStubElement<T, E>, T extends LSFExtend<T, E>> Query<T> findExtendElements(final LSFFullNameDeclaration decl, ExtendStubElementType<T, E> type, LSFFile file) {
        return findExtendElements(decl, type, file.getProject(), getRequireScope(file));
    }

    public static <E extends ExtendStubElement<T, E>, T extends LSFExtend<T, E>> Query<T> findExtendElements(final LSFFullNameDeclaration decl, ExtendStubElementType<T, E> type, Project project, GlobalSearchScope scope) {
        if (decl == null)
            return new EmptyQuery<>();

        StringStubIndexExtension<T> index = type.getGlobalIndex();

        String name = decl.getGlobalName();

//        int elementOffset = element.getTextOffset();
        Collection<T> decls = index.get(name, project, scope);

        return new FilteredQuery<>(new CollectionQuery<>(decls), new Condition<T>() {
            public boolean value(T t) {
                LSFFullNameDeclaration resolveDecl = t.resolveDecl();
                return resolveDecl != null && resolveDecl.equals(decl); // проверяем что resolve'ся куда надо
            }
        });
    }

    public static Collection<LSFClassDeclaration> findClassExtends(LSFClassDeclaration decl, Project project, GlobalSearchScope scope) {
        ClassExtendsClassIndex index = ClassExtendsClassIndex.getInstance();

        String name = decl.getGlobalName();

        Collection<LSFClassExtend> classExtends = index.get(name, project, scope);

        Collection<LSFClassDeclaration> result = new ArrayList<>();
        for (LSFClassExtend classExtend : classExtends) {
            boolean found = false;
            for (LSFClassDeclaration classExtendClass : classExtend.resolveExtends())
                if (classExtendClass.equals(decl)) { // проверяем что resolve'ся куда надо
                    found = true;
                    break;
                }
            if (found) {
                LSFClassDeclaration thisDecl = (LSFClassDeclaration) classExtend.resolveDecl();
                if (thisDecl != null)
                    result.add(thisDecl);
            }
        }
        return result;
    }
}

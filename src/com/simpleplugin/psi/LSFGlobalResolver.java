package com.simpleplugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.util.*;
import com.intellij.util.containers.ConcurrentHashMap;
import com.simpleplugin.BaseUtils;
import com.simpleplugin.psi.declarations.*;
import com.simpleplugin.psi.extend.LSFClassExtend;
import com.simpleplugin.psi.extend.LSFExtend;
import com.simpleplugin.psi.references.LSFModuleReference;
import com.simpleplugin.psi.references.LSFNamespaceReference;
import com.simpleplugin.psi.stubs.FullNameStubElement;
import com.simpleplugin.psi.stubs.extend.ExtendStubElement;
import com.simpleplugin.psi.stubs.extend.types.ExtendStubElementType;
import com.simpleplugin.psi.stubs.extend.types.indexes.ClassExtendsClassIndex;
import com.simpleplugin.psi.stubs.types.FullNameStubElementType;
import com.simpleplugin.psi.stubs.types.LSFStubElementTypes;

import java.util.*;

public class LSFGlobalResolver {

    public static ConcurrentHashMap<LSFModuleDeclaration, Set<VirtualFile>> cached = new ConcurrentHashMap<LSFModuleDeclaration, Set<VirtualFile>>();
    // вот эту хрень надо по хорошему кэшировать
    private static Set<VirtualFile> getRequireModules(LSFModuleDeclaration declaration) {
        Set<VirtualFile> cachedFiles = cached.get(declaration);
        if(cachedFiles!=null)
            return cachedFiles;

        Set<VirtualFile> result = new HashSet<VirtualFile>();

        result.add(declaration.getContainingFile().getVirtualFile());
        for(LSFModuleReference ref : declaration.getRequireRefs()) {
            LSFModuleDeclaration resolve = ref.resolveDecl();
            if(resolve!=null)
                result.addAll(getRequireModules(resolve));
        }
//        System.out.println("CACHED "+declaration.getName()+" "+System.identityHashCode(declaration));
        cached.put(declaration, result);
        return result;
    }

    private static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findInNamespace(Collection<T> decls, Finalizer<T> finalizer) {
        if(decls==null)
            return null;

        if(decls.size()>0)
            return finalizer.finalize(decls);
        return null;
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, String fqName, LSFFile file, Collection<FullNameStubElementType> types, Condition<T> condition, Finalizer<T> finalizer) {
        return findElements(name, fqName, file, BaseUtils.<Collection<FullNameStubElementType<S, T>>>immutableCast(types), null, condition, finalizer);
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, final String fqName, LSFFile file, Collection<? extends FullNameStubElementType<S, T>> types, T virtDecl, Condition<T> condition) {
        return findElements(name, fqName, file, types, virtDecl, condition, Finalizer.EMPTY);
    }
    
    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, final String fqName, LSFFile file, Collection<? extends FullNameStubElementType<S, T>> types, T virtDecl, Condition<T> condition, Finalizer<T> finalizer) {
        if(fqName!=null) {
            final Condition<T> fCondition = condition;
            condition = new Condition<T>() {
                public boolean value(T t) {
                    String namespace = t.getNamespaceName();
                    return namespace.equals(fqName) && fCondition.value(t);
                }
            };
        } else
            finalizer = new NamespaceFinalizer<T>(file.getModuleDeclaration(), finalizer);
        return findElements(name, file, types, virtDecl, condition, finalizer);  
    }
    
    private static class NamespaceFinalizer<T extends LSFFullNameDeclaration> implements Finalizer<T> {

        private final LSFModuleDeclaration moduleDeclaration;
        private final Finalizer<T> finalizer;

        private NamespaceFinalizer(LSFModuleDeclaration moduleDeclaration, Finalizer<T> finalizer) {
            this.moduleDeclaration = moduleDeclaration;
            this.finalizer = finalizer;
        }

        public Collection<T> finalize(Collection<T> decls) {
            if(decls.size()==1) // оптимизация
                return decls;
            
            Map<String, Collection<T>> mapDecls = new HashMap<String, Collection<T>>();
            for(T decl : decls) {
                String namespace = decl.getNamespaceName();

                Collection<T> nameList = mapDecls.get(namespace);
                if(nameList==null) {
                    nameList = new ArrayList<T>();
                    mapDecls.put(namespace, nameList);
                }
                nameList.add(decl);
            }

            // смотрим на priority
            Collection<T> result = findInNamespace(mapDecls.get(moduleDeclaration.getNamespace()), finalizer);
            if(result!=null)
                return result;

            for(LSFNamespaceReference priority : moduleDeclaration.getPriorityRefs()) {
                String resolve = priority.getNameRef();
                result = findInNamespace(mapDecls.get(resolve), finalizer);
                if(result!=null)
                    return result;
            }
            
            return finalizer.finalize(decls);
        }
    }

    public static <S extends FullNameStubElement<S, T>, T extends LSFFullNameDeclaration<T, S>> Collection<T> findElements(String name, LSFFile file, Collection<? extends FullNameStubElementType<S, T>> types, T virtDecl, Condition<T> condition, Finalizer<T> finalizer) {

        LSFModuleDeclaration moduleDeclaration = file.getModuleDeclaration();
        Project project = file.getProject();
        GlobalSearchScope scope = GlobalSearchScope.filesScope(project, getRequireModules(moduleDeclaration));

        Collection<T> decls = new ArrayList<T>();
        for(FullNameStubElementType<S, T> type : types)
            decls.addAll(type.getGlobalIndex().get(name, project, scope));
        if(virtDecl!=null)
            decls.add(virtDecl);

        Collection<T> fitDecls = new ArrayList<T>();
        for(T decl : decls) 
            if(condition.value(decl))
                fitDecls.add(decl);

        return finalizer.finalize(fitDecls);
    }

    public static Query<LSFModuleDeclaration> findModules(String name, GlobalSearchScope scope) {
        StringStubIndexExtension<LSFModuleDeclaration> index = LSFStubElementTypes.MODULE.getGlobalIndex();
        Collection<LSFModuleDeclaration> declarations = index.get(name, scope.getProject(), scope);
        return new CollectionQuery<LSFModuleDeclaration>(declarations);
    }

    public static Query<LSFNamespaceDeclaration> findNamespaces(String name, GlobalSearchScope scope) {
        
        Query<LSFNamespaceDeclaration> modules = BaseUtils.<LSFNamespaceDeclaration, LSFModuleDeclaration>immutableCast(findModules(name, scope));
        if(modules.findFirst()!=null)
            return modules;

        // модуля нет, ищем namespace'ы
        StringStubIndexExtension<LSFExplicitNamespaceDeclaration> explicitIndex = LSFStubElementTypes.EXPLICIT_NAMESPACE.getGlobalIndex();
        Collection<LSFExplicitNamespaceDeclaration> explicitDeclarations = explicitIndex.get(name, scope.getProject(), scope);
        if(explicitDeclarations.size() == 0)
            return modules;

        LSFExplicitNamespaceDeclaration minDeclaration = null;
        String minName = null;
        for(LSFExplicitNamespaceDeclaration explicitDecl : explicitDeclarations) {
            String moduleName = explicitDecl.getLSFFile().getModuleDeclaration().getDeclName();
            if(minName == null || minName.compareTo(moduleName) > 0) {
                minDeclaration = explicitDecl;
                minName = moduleName;
            }
        }
        return new MergeQuery<LSFNamespaceDeclaration>(modules, new CollectionQuery<LSFNamespaceDeclaration>(Collections.<LSFNamespaceDeclaration>singleton(minDeclaration)));
    }
    
    // этот элемент и все "выше"
    public static <E extends ExtendStubElement<T, E>, T extends LSFExtend<T, E>> Query<T> findExtendElements(T element) {
        return findExtendElements(element.resolveDecl(), (ExtendStubElementType<T, E>) element.getElementType(), element.getLSFFile());
    }
    public static <E extends ExtendStubElement<T, E>, T extends LSFExtend<T, E>> Query<T> findExtendElements(final LSFFullNameDeclaration decl, ExtendStubElementType<T, E> type, LSFFile file) {
        Project project = file.getProject();
        return findExtendElements(decl, type, project, GlobalSearchScope.filesScope(project, getRequireModules(file.getModuleDeclaration())));
    }
    public static <E extends ExtendStubElement<T, E>, T extends LSFExtend<T, E>> Query<T> findExtendElements(final LSFFullNameDeclaration decl, ExtendStubElementType<T, E> type, Project project, GlobalSearchScope scope) {
        if(decl==null)
            return new EmptyQuery<T>();

        StringStubIndexExtension<T> index = type.getGlobalIndex();

        String name = decl.getGlobalName();

//        int elementOffset = element.getTextOffset();
        Collection<T> decls = index.get(name, project, scope);

        return new FilteredQuery<T>(new CollectionQuery<T>(decls), new Condition<T>() {
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

        Collection<LSFClassDeclaration> result = new ArrayList<LSFClassDeclaration>();
        for(LSFClassExtend classExtend : classExtends) {
            boolean found = false;
            for(LSFClassDeclaration classExtendClass : classExtend.resolveExtends())
                if(classExtendClass.equals(decl)) { // проверяем что resolve'ся куда надо
                    found = true;
                    break;                            
                }
            if(found) {
                LSFClassDeclaration thisDecl = (LSFClassDeclaration)classExtend.resolveDecl();
                if(thisDecl!=null)
                    result.add(thisDecl);
            }                
        }
        return result;
    }
}

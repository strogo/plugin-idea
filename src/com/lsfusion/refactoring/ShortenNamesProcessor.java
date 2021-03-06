package com.lsfusion.refactoring;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.lsfusion.lang.LSFElementGenerator;
import com.lsfusion.lang.LSFFileType;
import com.lsfusion.lang.classes.*;
import com.lsfusion.lang.meta.MetaChangeDetector;
import com.lsfusion.lang.meta.MetaTransaction;
import com.lsfusion.lang.psi.*;
import com.lsfusion.lang.psi.declarations.*;
import com.lsfusion.lang.psi.declarations.impl.LSFActionOrGlobalPropDeclarationImpl;
import com.lsfusion.lang.psi.indexes.ModuleIndex;
import com.lsfusion.lang.psi.references.*;
import com.lsfusion.lang.psi.references.impl.LSFPropReferenceImpl;
import com.lsfusion.util.BaseUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.lsfusion.util.LSFPsiUtils.collectInjectedLSFFiles;

public class ShortenNamesProcessor {

    private static boolean isPredefinedWord(String s) {
        return s.equals("VAT") || s.equals("UOM");
    }

    public static List<String> getWords(String string) {
        if(string.isEmpty())
            return new ArrayList<>();
            
        List<String> result = new ArrayList<>();
        int prevStart = 0;
        for(int i=1;i<string.length();i++) {
            if(Character.isUpperCase(string.charAt(i)) && !(Character.isUpperCase(string.charAt(i-1)) && (i+1>=string.length() || Character.isUpperCase(string.charAt(i + 1))) && !isPredefinedWord(string.substring(prevStart, i)))) {
                if(prevStart >= 0)
                    result.add(string.substring(prevStart, i));
                prevStart = i;
            }
        }
        if(prevStart >= 0)
            result.add(string.substring(prevStart, string.length()));
        return result;
    }

    private static String toWords(List<String> words) {
        String result = "";
        for(String word : words)
            result += word;
        return result;
    }

    private static String shortenName(LSFActionOrPropDeclaration decl) {
        String declName = decl.getDeclName();
        
        List<String> declWords = getWords(declName);
        int keep = declWords.size() - 1;
        
        List<String> result = new ArrayList<>();

        List<String> paramNames = null;
        if(decl instanceof LSFActionOrGlobalPropDeclarationImpl)
            paramNames = ((LSFActionOrGlobalPropDeclarationImpl)decl).resolveParamNames();

        boolean firstAte = false;
        List<LSFClassSet> paramClasses = decl.resolveParamClasses();
        if(paramClasses!=null || paramNames!=null) {
            int size = (paramClasses != null ? paramClasses.size() : paramNames.size());
            for(int i= size-1;i>=0;i--) {
                LSFClassSet prmClass = null;
                if(paramClasses!=null)
                    prmClass = paramClasses.get(i);
                String prmName = null;
                if(paramNames!=null)
                    prmName = paramNames.get(i);

                if(i==0 && size == 1 && (declName.equals("toDate") || declName.equals("toTime") || declName.equals("toDateTime") || declName.equals("objectClass"))) {
                    String prmCheck = null;
                    if(prmName!=null)
                        prmCheck = prmName.toUpperCase();
                    if(prmClass!=null)
                        prmCheck = prmClass.getCommonClass().getName();
                    if(prmCheck != null) {
                        if((declName.equals("toDate") && (prmCheck.equals(DateTimeClass.instance.getName()) || prmCheck.equals(ZDateTimeClass.instance.getName()))) ||
                            (declName.equals("toTime") && (prmCheck.equals(DateTimeClass.instance.getName()) || prmCheck.equals(ZDateTimeClass.instance.getName()))) ||
                            (declName.equals("toDateTime") && prmCheck.equals(DateClass.instance.getName())) ||
                            (declName.equals("objectClass") && prmCheck.equals("Object")))
                            return declName;                            
                    }
                }

                int k = 0; // сколько слов нашли                    
                int skip = 0; // сколько слов пропустили
                int skipFull = 0; // сколько полных слов пропустили
                while(k == 0) {
                    List<String> sNames = new ArrayList<>();
                    if(prmClass!=null)
                        sNames = prmClass.getCommonClass().getSNames();
                    if(paramNames!=null)
                        sNames = BaseUtils.add(sNames, StringUtils.capitalize(prmName));
                    for(String sname : sNames) {
                        k = equalWords(declWords, keep - skip, sname);
                        if(result.size()==0 && skip == 0 && k > keep) // если все съели, оставим одно слово
                            k = k-1;
                        if(k > 0)
                            break;
                    }
                    
                    if(k == 0) {
                        if(keep - skip >= 0) {
                            if(declWords.get(keep - skip).length() > 1)
                                skipFull++;
                        } else
                            keep = keep;
                        skip++;
                    }

                    if((keep - skip) < 0)
                        break;
                    
                    if(skipFull > 3 && !(k==0 && skipFull == 4 && equalWord(declWords.get(keep - skip + 1), "length", true) && equalWord(declWords.get(keep - skip + 2), "leg", true))) // пропускаем не больше 3 слов или lengthLegInsideSurface
                        break;
                }
                
                if(k > 0) { // нашли
                    for(int u=0;u<skip;u++)
                        result.add(declWords.get(keep - u));
                    keep -= skip;
                    if(k > keep) // если съели первую
                        firstAte = true;    
                        
                    keep -= k;
                }
            }
        }
        
        result.addAll(BaseUtils.reverse(declWords.subList(0, keep+1)));
        if(firstAte) {
            String s = result.get(result.size() - 1);
            if(s.length() > 1)
                result.set(result.size() - 1, s.toLowerCase());
        }
        return toWords(BaseUtils.reverse(result));
    }

    private static int equalWords(List<String> declWords, int keep, String sname) {
        List<String> prmWords = getWords(sname);
        int j = prmWords.size() - 1;
        int k = 0;
        int skipPrm = 0;
        while(true) {
            while(j - skipPrm - k >= 0 && keep - k >= 0 && equalWord(declWords.get(keep - k), prmWords.get(j - skipPrm - k), false)) {
                k++;
            }
            skipPrm++;
            if(k>0 || skipPrm > j)
                break;                                                         
        }
        return k;
    }

    private static boolean equalWord(String first, String second, boolean isFirst) {
        return first.equals(second) || (isFirst && first.toLowerCase().equals(second.toLowerCase()));
    }

    public static void shortenAllPropNames(Project project) {
        shortenAllPropNames(project, MigrationChangePolicy.INCREMENT_VERSION);
    }

    public static void shortenAllPropNames(Project project, MigrationChangePolicy migrationChangePolicy) {
        GlobalSearchScope scope = ProjectScope.getProjectScope(project);

        final List<LSFFile> files = new ArrayList<>();

        for (VirtualFile lsfFile : FileTypeIndex.getFiles(LSFFileType.INSTANCE, scope)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(lsfFile);
            if (psiFile instanceof LSFFile) {
                files.add((LSFFile) psiFile);
            }
        }
        
        for (VirtualFile javaFile : FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)) {
            files.addAll(collectInjectedLSFFiles(javaFile, project));
        }

        for (VirtualFile xmlFile : FileTypeIndex.getFiles(XmlFileType.INSTANCE, scope)) {
            if (xmlFile.getName().endsWith("jrxml")) {
                files.addAll(collectInjectedLSFFiles(xmlFile, project));
            }
        }
        
        List<ElementMigration> migrations = new ArrayList<>();

        MetaTransaction transaction = new MetaTransaction();

        shortenAllPropNames(files, migrations, transaction);

        transaction.apply();
        
        if (migrationChangePolicy != MigrationChangePolicy.DO_NOT_MODIFY) {
            MigrationScriptUtils.modifyMigrationScripts(migrations, migrationChangePolicy, ProjectScope.getProjectScope(project));
        }
    }

    public static void shortenAllPropNames(Collection<LSFFile> files, List<ElementMigration> migrations, MetaTransaction transaction) {

        Project project = files.iterator().next().getProject();
        MetaChangeDetector.getInstance(project).setMetaEnabled(false, false);

        Map<LSFActionOrPropDeclaration, String> propertyDecls = new HashMap<>();
        Map<LSFPropertyDrawDeclaration, String> propertyDrawDecls = new HashMap<>();
        
        System.out.println("Collecting property decls...");
        int i = 0;
        for(LSFFile file : files) {
            i++;
            for(LSFActionOrPropDeclaration decl : PsiTreeUtil.findChildrenOfType(file, LSFActionOrPropDeclaration.class))
                propertyDecls.put(decl, shortenName(decl));
            if (i % 1000 == 0) {
                System.out.println(i + " of " + files.size() + ": " + (double) i / ((double) files.size()));
            }
        }

        System.out.println("Collecting and resolving property refs...");

        List<ExtPropRef> propRefs = new ArrayList<>();
        i = 0;
        for (LSFFile file : files) {
            i++;
            for (LSFActionOrPropReference<?, ?> ref : PsiTreeUtil.findChildrenOfType(file, LSFActionOrPropReference.class)) {
                LSFActionOrPropDeclaration decl = ref.resolveDecl();
                if (decl != null) {
                    List<LSFClassSet> paramClasses = decl.resolveParamClasses();
                    if (paramClasses != null) {
                        propRefs.add(new ExtPropRef(ref, decl, paramClasses, ref.hasExplicitClasses(), ref.getFullNameRef() != null));
                    }
                }
            }
            if (i % 1000 == 0) {
                System.out.println(i + " of " + files.size() + ": " + (double) i / ((double) files.size()));
            }
        }
        
        System.out.println("Collecting property draw decls...");
        i = 0;
        for(ExtPropRef propRef : propRefs) {
            i++;

            PsiElement refParent = PsiTreeUtil.getParentOfType(propRef.ref.getElement(), LSFPropertyDrawDeclaration.class, LSFFormPropertyOptionsList.class);

            if (refParent instanceof LSFPropertyDrawDeclaration) {
                LSFPropertyDrawDeclaration drawDecl = (LSFPropertyDrawDeclaration) refParent;
                if (drawDecl.getSimpleName() == null) {
                    String newName = propertyDecls.get(propRef.decl);
                    propertyDrawDecls.put(drawDecl, newName);
                }
            }

            if (i % 1000 == 0) {
                System.out.println(i + " of " + propRefs.size() + ": " + (double) i / ((double) propRefs.size()));
            }
        }

        System.out.println("Collecting and resolving property draw refs...");
        List<ExtPropDrawRef> propDrawRefs = new ArrayList<>();
        i = 0;
        for(LSFFile file : files) {
            i++;
            for(LSFPropertyDrawReference ref : PsiTreeUtil.findChildrenOfType(file, LSFPropertyDrawReference.class)) {
                if (ref.getAliasUsage() == null) {
                    LSFPropertyDrawDeclaration decl = ref.resolveDecl();
                    if (decl != null && decl.getSimpleName() == null) {
                        propDrawRefs.add(new ExtPropDrawRef(ref, decl));
                    }
                }
            }
            if (i % 1000 == 0) {
                System.out.println(i + " of " + files.size() + ": " + (double) i / ((double) files.size()));
            }
        }

        System.out.println("Migrating property draws..."); // отдельно для кэширования, так как resolve вызывается
        i = 0;
        for(Map.Entry<LSFPropertyDrawDeclaration, String> e : propertyDrawDecls.entrySet()) {
            LSFPropertyDrawDeclaration decl = e.getKey();
            String newName = e.getValue();

            String oldName = decl.getDeclName();
            if (!oldName.equals(newName)) {
                assert decl.getSimpleName() == null;
                PropertyDrawMigration migration = new PropertyDrawMigration(decl, newName);
                assert migration != null;
                migrations.add(migration);
            }

            i++;
            if (i % 1000 == 0) {
                System.out.println(i + " of " + propertyDecls.size() + ": " + (double) i / ((double) propertyDecls.size()));
            }
        }

        System.out.println("Migrating decls..."); // отдельно так как resolve вызывается, но нужно вызывать до изменения psi так как используется resolveClasses
        i = 0;
        for(Map.Entry<LSFActionOrPropDeclaration, String> e : propertyDecls.entrySet()) {
            LSFActionOrPropDeclaration decl = e.getKey();
            String newName = e.getValue();

            if (decl instanceof LSFActionOrGlobalPropDeclaration) {
                LSFActionOrGlobalPropDeclaration globalDecl = (LSFActionOrGlobalPropDeclaration) decl;
                String oldName = decl.getDeclName();
                if (!oldName.equals(newName)) {
                    migrations.add(new PropertyMigration(globalDecl, oldName, newName));
                }
            }

            i++;
            if (i % 1000 == 0) {
                System.out.println(i + " of " + propertyDecls.size() + ": " + (double) i / ((double) propertyDecls.size()));
            }
        }

        System.out.println("Renaming property draw refs...");
        i = 0;
        for(ExtPropDrawRef propDrawRef : propDrawRefs) {
            i++;

            String newPropDrawName = propertyDrawDecls.get(propDrawRef.decl);
            if (newPropDrawName != null) {
                propDrawRef.ref.handleElementRename(newPropDrawName, transaction);
            }

            if (i % 1000 == 0) {
                System.out.println(i + " of " + propRefs.size() + ": " + (double) i / ((double) propRefs.size()));
            }
        }

        System.out.println("Sorting property refs...");
        final InjectedLanguageManager injectionManager = InjectedLanguageManagerImpl.getInstance(project);
        Collections.sort(propRefs, new Comparator<ExtPropRef>() {
            @Override
            public int compare(ExtPropRef o1, ExtPropRef o2) {
                PsiLanguageInjectionHost host1 = injectionManager.getInjectionHost(o1.ref);
                PsiLanguageInjectionHost host2 = injectionManager.getInjectionHost(o2.ref);
                if (host1 == null && host2 != null) return 1;
                if (host1 != null && host2 == null) return -1;
                if (host1 == null && host2 == null) return 0;
                
                PsiFile f1 = host1.getContainingFile();
                PsiFile f2 = host2.getContainingFile();
                
                if (f1 != f2) {
                    //в разных файлах - не важно как сортировать
                    return Integer.compare(System.identityHashCode(f1), System.identityHashCode(f2));
                }

                //позже должен обрабатываться тот, который выше по файлу
                return -Integer.compare(host1.getTextOffset(), host2.getTextOffset());
            }
        });

        System.out.println("Renaming and qualifying property refs...");
        i = 0;
        for(ExtPropRef resolvedRef : propRefs) {
            i++;

            String newPropName = propertyDecls.get(resolvedRef.decl);

            LSFActionOrPropReference propRef = resolvedRef.ref;

            propRef.handleElementRename(newPropName, transaction);
            if(!resolvedRef.qualClasses)
                propRef.setExplicitClasses(resolvedRef.classes, transaction);
            if(!resolvedRef.qualNamespace && (resolvedRef.decl instanceof LSFFullNameDeclaration))
                propRef.setFullNameRef(((LSFFullNameDeclaration) resolvedRef.decl).getNamespaceName(), transaction);

            if (i % 1000 == 0) {
                System.out.println(i + " of " + propRefs.size() + ": " + (double) i / ((double) propRefs.size()));
            }
        }

        System.out.println("Renaming decls...");
        i = 0;
        for(Map.Entry<LSFActionOrPropDeclaration, String> e : propertyDecls.entrySet()) {
            LSFActionOrPropDeclaration decl = e.getKey();
            String newName = e.getValue();
            i++;
            decl.setName(newName, transaction);
            if (i % 1000 == 0) {
                System.out.println(i + " of " + propertyDecls.size() + ": " + (double) i / ((double) propertyDecls.size()));
            }
        }

        System.out.println("Unqualifying refs...");
        i = 0;
        for(ExtPropRef possibleConflict : propRefs) {
            i++;
            unqualifyConflict(possibleConflict.ref, possibleConflict.decl, possibleConflict.classes, transaction);
            if (i % 1000 == 0) {
                System.out.println(i + " of " + propRefs.size() + ": " + (double) i / ((double) propRefs.size()));
            }
        }
    }

    static void unqualifyConflict(LSFActionOrPropReference ref, LSFActionOrPropDeclaration decl, List<LSFClassSet> explicitClasses, MetaTransaction transaction) {
        if(decl instanceof LSFFullNameDeclaration && ref.getFullNameRef() != null) {
            ref.dropFullNameRef(transaction);
            if(ref.resolveDecl() != decl)
                ref.setFullNameRef(((LSFFullNameDeclaration) decl).getNamespaceName(), transaction);
        }
        
        if(ref.getExplicitClasses() != null && !ref.isNoContext()) {
            List<LSFClassSet> declClasses = decl.resolveParamClasses();
            if(declClasses != null)
                ref.dropExplicitClasses(transaction);
    
            if(ref.resolveDecl() != decl)
                ref.setExplicitClasses(explicitClasses, transaction);
        }
    }

    public static void unqualifyRefs(Collection<LSFFile> files, MetaTransaction transaction) {
        System.out.println("Collecting and resolving refs...");
        int i = 0;
        for(LSFFile file : files) {
            i++;
            for(LSFActionOrPropReference<?, ?> ref : PsiTreeUtil.findChildrenOfType(file, LSFActionOrPropReference.class)) {
                if(ref.getExplicitClasses()!=null || ref.getFullNameRef()!=null) {
                    LSFActionOrPropDeclaration decl = ref.resolveDecl();
                    if(decl!=null) {
                        List<LSFClassSet> declClasses = decl.resolveParamClasses();
                        if(declClasses!=null && declClasses.size() > 0)
                            unqualifyConflict(ref, decl, declClasses, transaction);
                    }
                }
            }
            System.out.println((double)i/((double)files.size()));
        }
    }

    private static class ExtPropRef {
        public final LSFActionOrPropReference ref;
        public final LSFActionOrPropDeclaration decl;
        public final List<LSFClassSet> classes;
        public final boolean qualClasses;
        public final boolean qualNamespace;

        private ExtPropRef(LSFActionOrPropReference ref, LSFActionOrPropDeclaration decl, List<LSFClassSet> classes, boolean qualClasses, boolean qualNamespace) {
            this.ref = ref;
            this.decl = decl;
            this.classes = classes;
            this.qualClasses = qualClasses;
            this.qualNamespace = qualNamespace;
        }
    }

    private static class ExtPropDrawRef {
        public final LSFPropertyDrawReference ref;
        public final LSFPropertyDrawDeclaration decl;

        private ExtPropDrawRef(LSFPropertyDrawReference ref, LSFPropertyDrawDeclaration decl) {
            this.ref = ref;
            this.decl = decl;
        }
    }

    private static abstract class NameRef<D extends LSFDeclaration, R extends LSFReference<D>> {
        protected String name;
        
        public boolean equalsDecl(D decl) {
            return name == null || decl.getDeclName().equals(name);
        }
        
        protected boolean isEmpty() { // оптимизация
            return name == null;
        }
        public boolean equalsRef(R ref, D decl) {
            if(name!=null && !name.equals(ref.getNameRef())) // оптимизация
                return false;
            
            if(isEmpty()) // оптимизация
                return true;
            
            if(decl==null) // оптимизация 
                decl = ref.resolveDecl();
            return decl != null && equalsDecl(decl);
        }

        protected NameRef(String name) {
            this.name = name;
        }
    }

    private static class FullNameRef<D extends LSFDeclaration, FD extends LSFFullNameDeclaration<FD, ?>, R extends LSFFullNameReference<D, FD>> extends NameRef<D, R> {
        protected String namespace;

        private FullNameRef(String name, String namespace) {
            super(name);
            this.namespace = namespace;
        }

        protected boolean isFullName(D decl) {
            return true;
        }

        public boolean equalsFullDecl(FD decl) {
            return namespace == null || namespace.equals(decl.getNamespaceName());
        }

        @Override
        protected boolean isEmpty() {
            return super.isEmpty() && namespace == null;
        }

        @Override
        public boolean equalsDecl(D decl) {
            return super.equalsDecl(decl) && (isFullName(decl) && equalsFullDecl((FD) decl));
        }
    }

    private static class ClassRef extends FullNameRef<LSFClassDeclaration, LSFClassDeclaration, LSFClassReference> {
        
        public boolean equalsValueClass(LSFValueClass valueClass) {
            if(valueClass instanceof LSFClassDeclaration)
                return equalsDecl((LSFClassDeclaration)valueClass);

            if(name == null)
                return true;

            if(valueClass instanceof ConcatenateClassSet)
                return name.equals("STRUCT");
            if(valueClass instanceof DataClass)
                return name.equals(((DataClass) valueClass).getName());
            
            throw new UnsupportedOperationException();
        }

        private ClassRef(String name, String namespace) {
            super(name, namespace);
        }
    }

    private static class MetacodeRef extends FullNameRef<LSFMetaDeclaration, LSFMetaDeclaration, LSFMetaReference> {
        private Integer metaCount;

        @Override
        public boolean equalsFullDecl(LSFMetaDeclaration decl) {
            return super.equalsFullDecl(decl) && (metaCount == null || decl.getParamCount() == metaCount);
        }

        private MetacodeRef(String name, String namespace, Integer metaCount) {
            super(name, namespace);
            this.metaCount = metaCount;
        }

        @Override
        protected boolean isEmpty() {
            return super.isEmpty() && metaCount == null;
        }
    }

    private interface CalcGraph {
        void proceedFile(Map<PropId, Set<PropId>> result, LSFFile file);        
    }
    
    private static class PropId {
        private final String file;
        private final TextRange range;
        
        private final String canonicalName;

        private PropId(LSFActionOrGlobalPropDeclaration decl) {
            this.file = decl.getLSFFile().getName();
            this.range = decl.getTextRange();

            canonicalName = PropertyCanonicalNameUtils.createName(decl.getNamespaceName(), decl.getDeclName(), decl.resolveParamClasses());
        }

        @Override
        public String toString() {
            return canonicalName + " " + file + " " + range;            
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PropId propId = (PropId) o;

            if (!file.equals(propId.file)) return false;
            if (!range.equals(propId.range)) return false;

            assert canonicalName.equals(propId.canonicalName);
            return true;
        }

        @Override
        public int hashCode() {
            int result = file.hashCode();
            result = 31 * result + range.hashCode();
            return result;
        }
    }

    private static Set<PropId> recCalcGraph(PropId decl, Map<PropId, Set<PropId>> graph) {
        Set<PropId> result = new HashSet<>();
        
        Set<PropId> setGraph = graph.get(decl);        
        if(setGraph != null) {
            for(PropId prop : setGraph) {
                result.addAll(recCalcGraph(prop, graph));
                result.add(prop);
            }
        }
        return result;
    }

    public static Map<PropId, Set<PropId>> calcGraph(Project myProject, CalcGraph calcGraph) {
        Collection<String> allKeys = ModuleIndex.getInstance().getAllKeys(myProject);

        Map<PropId, Set<PropId>> result = new HashMap<>();
                
        for (final String module : allKeys) {
            Collection<LSFModuleDeclaration> moduleDeclarations = ModuleIndex.getInstance().get(module, myProject, GlobalSearchScope.allScope(myProject));
            for (LSFModuleDeclaration declaration : moduleDeclarations) {
                calcGraph.proceedFile(result, declaration.getLSFFile());
            }
        }
     
        Map<PropId, Set<PropId>> fullGraph = new HashMap<>();
        for(Map.Entry<PropId, Set<PropId>> entry : result.entrySet()) {
            fullGraph.put(entry.getKey(), recCalcGraph(entry.getKey(), result));
        }
        return fullGraph;
    }

    public static Map<PropId, Set<PropId>> calcAbstractGraph(Project myProject, final Set<PropId> props) {
        return calcGraph(myProject, new CalcGraph() {
            public void proceedFile(Map<PropId, Set<PropId>> result, LSFFile file) {
                LSFFile projectFile = LSFElementGenerator.createProjectLSFFile(file);
                for (PsiElement statement : PsiTreeUtil.findChildrenOfType(file, LSFActionOrGlobalPropDeclaration.class)) {
                    LSFActionOrGlobalPropDeclaration propStatement = (LSFActionOrGlobalPropDeclaration) statement;
                    
                    PropId propId = new PropId(propStatement);
                    props.add(propId);
                    
                    List<LSFClassSet> paramClasses = propStatement.resolveParamClasses();

                    if (paramClasses != null) { // эмулируем +=
                        LSFPropReferenceImpl ref = LSFElementGenerator.createImplementPropRefFromText(propStatement.getDeclName(), projectFile, paramClasses);
                        LSFResolveResult resResult = ref.multiResolveDecl(false);
                        if (!(resResult.errorAnnotator instanceof LSFResolveResult.NotFoundErrorAnnotator)) {
                            if(!resResult.declarations.contains(propStatement)) {
                                for (LSFDeclaration decl : resResult.declarations) {
                                    PropId abstId = new PropId((LSFActionOrGlobalPropDeclaration) decl);
                                    Set<PropId> impls = result.get(abstId);
                                    if (impls == null) {
                                        impls = new HashSet<>();
                                        result.put(abstId, impls);
                                    }
                                    impls.add(propId);
                                }
                            } else {
                                LSFPropReferenceImpl ref2 = LSFElementGenerator.createImplementPropRefFromText(propStatement.getDeclName(), projectFile, paramClasses);
                                LSFResolveResult resResult2 = ref2.multiResolveDecl(false);
                                assert resResult.declarations.size() == 1 && propStatement.isAbstract();
                            }
                        }
                    }
                }
            }
        });
    }
    
    private static boolean isCompleteElement(PsiElement outer, PsiElement inner) {
        return outer.getTextRange().equals(inner.getTextRange());
    }

    public static Map<PropId, Set<PropId>> calcImplementGraph(Project myProject) {
        return calcGraph(myProject, new CalcGraph() {
            public void proceedFile(Map<PropId, Set<PropId>> result, LSFFile file) {
                for (PsiElement statement : PsiTreeUtil.findChildrenOfType(file, LSFOverridePropertyStatement.class)) {

                    LSFOverridePropertyStatement ovStatement = (LSFOverridePropertyStatement) statement;
                    LSFMappedPropertyClassParamDeclare mappedDeclare = ovStatement.getMappedPropertyClassParamDeclare();
                    LSFPropertyUsage usage = mappedDeclare.getPropertyUsageWrapper().getPropertyUsage();
                    LSFGlobalPropDeclaration decl = (LSFGlobalPropDeclaration) usage.resolveDecl();
                    if(decl != null) {
                        List<LSFParamDeclaration> params = LSFPsiImplUtil.resolveParams(mappedDeclare.getClassParamDeclareList());
                        if (params != null) {
                            List<LSFPropertyExpression> peList = ovStatement.getPropertyExpressionList();
//                            LSFListActionPropertyDefinitionBody body = ovStatement.getListActionPropertyDefinitionBody();
//                            if (body != null) {
//                                if (peList.size() > 0)
//                                    break;
//                                proceedImpl(body, decl, params, result);
//                            } else {
                                if (peList.size() == 1) {
                                    LSFPropertyExpression pe = peList.get(0);
                                    proceedImpl(pe, decl, params, result);
                                }
//                            }
                        }
                    }
                }
            }
        });
    }

    private interface Getter<T> {
        Pair<LSFPropertyUsage, LSFPropertyExpressionList> get(T element);
    }
    private static void proceedImpl(LSFPropertyExpression pe, LSFGlobalPropDeclaration abst, List<LSFParamDeclaration> params, Map<PropId, Set<PropId>> result) {
        proceedImpl(pe, abst, params, LSFJoinPropertyDefinition.class, new Getter<LSFJoinPropertyDefinition>() {
            public Pair<LSFPropertyUsage, LSFPropertyExpressionList> get(LSFJoinPropertyDefinition element) {
                return Pair.create(element.getPropertyUsage(), element.getPropertyExpressionList());
            }
        }, result);
    }
    
    private static void proceedImpl(LSFListActionPropertyDefinitionBody pe, LSFGlobalPropDeclaration abst, List<LSFParamDeclaration> params, Map<PropId, Set<PropId>> result) {
        if(1==1)
            return;
        return;
//        proceedImpl(pe, abst, params, LSFExecActionPropertyDefinitionBody.class, new Getter<LSFExecActionPropertyDefinitionBody>() {
//            public Pair<LSFActionUsage, LSFPropertyExpressionList> get(LSFExecActionPropertyDefinitionBody element) {
//                return Pair.create(element.getActionUsage(), element.getPropertyExpressionList());
//            }
//        }, result);
    }
    
    private static <T extends PsiElement> void proceedImpl(PsiElement pe, LSFGlobalPropDeclaration abst, List<LSFParamDeclaration> params, Class<T> aClass, Getter<T> getter, Map<PropId, Set<PropId>> result) {
        assert pe instanceof LSFPropertyExpression || pe instanceof LSFActionPropertyDefinitionBody;
        String declName = abst.getDeclName();
        for (PsiElement element : PsiTreeUtil.findChildrenOfType(pe, aClass)) {
            // проверяем что целиком элемент
            if (!isCompleteElement(pe, element))
                break;

            Pair<LSFPropertyUsage, LSFPropertyExpressionList> usageWithExprs = getter.get((T) element);

            LSFPropertyUsage propUsage = usageWithExprs.first;
            if (propUsage != null) {
                String nameRef = propUsage.getNameRef();
                if (!nameRef.equals(declName)) // проверяем имя
                    break;

                // проверяем что подставляются только параметры в нужном порядке (просто по имени)                                        
                List<LSFPropertyExpression> joinList = LSFPsiImplUtil.getList(usageWithExprs.second);
                boolean parameterMatch = true;
                for (int i = 0; i < joinList.size(); i++) {
                    LSFPropertyExpression param = joinList.get(i);
                    for (PsiElement child : PsiTreeUtil.findChildrenOfType(param, LSFAbstractParamReference.class)) {
                        if (!isCompleteElement(param, child)) {
                            parameterMatch = false;
                            break;
                        }

                        LSFAbstractParamReference paramRef = (LSFAbstractParamReference) child;
                        LSFDeclaration paramDecl = paramRef.resolveDecl();
                        if (!params.get(i).equals(paramDecl)) {
                            parameterMatch = false;
                            break;
                        }

                        break;
                    }
                    if (!parameterMatch)
                        break;
                }

                if (parameterMatch) { // параметры тоже подходят
                    PropId abstId = new PropId(abst);
                    LSFGlobalPropDeclaration impl = (LSFGlobalPropDeclaration) propUsage.resolveDecl();
                    if (impl != null) {
                        Set<PropId> impls = result.get(abstId);
                        if (impls == null) {
                            impls = new HashSet<>();
                            result.put(abstId, impls);
                        }
                        impls.add(new PropId(impl));
                    }
                }
            }
            break;
        }
    }    
    
    public static void checkGraphs(Project myProject) {
        System.out.println("start");
        Set<PropId> props = new HashSet<>();
        Map<PropId, Set<PropId>> absGraph = calcAbstractGraph(myProject, props);
        Map<PropId, Set<PropId>> impGraph = calcImplementGraph(myProject);
        System.out.println("stop");
        
        for(PropId prop : props) {
            Set<PropId> absEdges = absGraph.get(prop);
            if(absEdges == null)
                absEdges = new HashSet<>();
            else
                absEdges = new HashSet<>(absEdges);
            Set<PropId> impEdges = impGraph.get(prop);
            if(impEdges == null)
                impEdges = new HashSet<>();
            else
                impEdges = new HashSet<>(impEdges);
            
            BaseUtils.split(absEdges, impEdges);
            
            for(PropId edge : absEdges) {
                System.out.println("ABS : " + prop + ", BUT NOT IMPL : " + edge);
            }

            for(PropId edge : impEdges) {
                System.out.println("IMPL : " + prop + ", BUT NOT ABS : " + edge);
            }
        }
    }

}

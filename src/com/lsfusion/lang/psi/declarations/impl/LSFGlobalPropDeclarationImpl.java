package com.lsfusion.lang.psi.declarations.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CollectionQuery;
import com.lsfusion.LSFIcons;
import com.lsfusion.lang.ColumnNamingPolicy;
import com.lsfusion.lang.classes.CustomClassSet;
import com.lsfusion.lang.classes.LSFClassSet;
import com.lsfusion.lang.classes.LSFValueClass;
import com.lsfusion.lang.psi.*;
import com.lsfusion.lang.psi.cache.*;
import com.lsfusion.lang.psi.declarations.*;
import com.lsfusion.lang.psi.indexes.TableClassesIndex;
import com.lsfusion.lang.psi.references.LSFPropReference;
import com.lsfusion.lang.psi.stubs.PropStubElement;
import com.lsfusion.lang.psi.stubs.types.FullNameStubElementType;
import com.lsfusion.lang.psi.stubs.types.LSFStubElementTypes;
import com.lsfusion.lang.typeinfer.InferExResult;
import com.lsfusion.lang.typeinfer.LSFExClassSet;
import com.lsfusion.refactoring.ElementMigration;
import com.lsfusion.refactoring.PropertyMigration;
import com.lsfusion.util.BaseUtils;
import com.lsfusion.util.LSFPsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public abstract class LSFGlobalPropDeclarationImpl extends LSFFullNameDeclarationImpl<LSFGlobalPropDeclaration, PropStubElement> implements LSFGlobalPropDeclaration {

    public static final String TABLE_BASE_PREFIX = "base";
    public static final String TABLE_BASE0 = TABLE_BASE_PREFIX + 0;

    public LSFGlobalPropDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    public LSFGlobalPropDeclarationImpl(@NotNull PropStubElement propStubElement, @NotNull IStubElementType nodeType) {
        super(propStubElement, nodeType);
    }

    @Override
    public List<String> resolveParamNames() {
        LSFPropertyDeclaration decl = getPropertyDeclaration();
        LSFPropertyDeclParams declList = decl.getPropertyDeclParams();
        List<LSFExprParamDeclaration> params = null;
        if(declList!=null)
            params = BaseUtils.<List<LSFExprParamDeclaration>>immutableCast(LSFPsiImplUtil.resolveParams(declList.getClassParamDeclareList()));
        else {
            LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
            if(pCalcStatement != null) {
                LSFPropertyExpression pe = pCalcStatement.getPropertyExpression();
                if (pe != null)
                    params = LSFPsiImplUtil.resolveParams(pe);
            }
        }
        return LSFPsiImplUtil.resolveParamNames(params);
    }

    public abstract LSFPropertyDeclaration getPropertyDeclaration();

    @Nullable
    public abstract LSFActionUnfriendlyPD getActionUnfriendlyPD();

    public abstract LSFListActionPropertyDefinitionBody getListActionPropertyDefinitionBody();

    @Nullable
    public abstract LSFPropertyCalcStatement getPropertyCalcStatement();

    @Nullable
    public abstract LSFNonEmptyPropertyOptions getNonEmptyPropertyOptions();

    @Override
    public boolean isAbstract() {
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            LSFExpressionUnfriendlyPD unfriend = pCalcStatement.getExpressionUnfriendlyPD();
            if (unfriend != null) {
                return unfriend.getAbstractPropertyDefinition() != null;
            }
        } else {
            LSFActionUnfriendlyPD unfriend = getActionUnfriendlyPD();
            if (unfriend != null) {
                return unfriend.getAbstractActionPropertyDefinition() != null;
            }
        }
        return false;
    }

    @Override
    public boolean isUnfriendly() {
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            return pCalcStatement.getExpressionUnfriendlyPD() != null;
        } else {
            return getActionUnfriendlyPD() != null;
        }
    }

    @Override
    @Nullable
    public LSFId getNameIdentifier() {
        return getPropertyDeclaration().getSimpleNameWithCaption().getSimpleName();
    }

    @Override
    public String getCaption() {
        LSFLocalizedStringLiteral stringLiteral = getPropertyDeclaration().getSimpleNameWithCaption().getLocalizedStringLiteral();
        return stringLiteral != null ? stringLiteral.getValue() : null;
    }

    @Override
    public LSFExClassSet resolveExValueClass(boolean infer) {
        return ValueClassCache.getInstance(getProject()).resolveValueClassWithCaching(this, infer);
    }

    @Override
    public LSFExClassSet resolveExValueClassNoCache(boolean infer) {
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            LSFExpressionUnfriendlyPD unfr = pCalcStatement.getExpressionUnfriendlyPD();
            if (unfr != null)
                return unfr.resolveUnfriendValueClass(infer);

            LSFPropertyExpression expr = pCalcStatement.getPropertyExpression();
            if (expr != null)
                return expr.resolveValueClass(infer);
        }

        return null;
    }

    @Nullable
    private List<LSFExClassSet> resolveValueParamClasses() {
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            LSFExpressionUnfriendlyPD unfr = pCalcStatement.getExpressionUnfriendlyPD();
            if (unfr != null)
                return unfr.resolveValueParamClasses();

            LSFPropertyExpression expr = pCalcStatement.getPropertyExpression();
            if (expr != null)
                return LSFExClassSet.toEx(LSFPsiImplUtil.resolveValueParamClasses(expr));
        } else {
            return LSFPsiImplUtil.resolveValueParamClasses(getActionUnfriendlyPD(), getListActionPropertyDefinitionBody());
        }

        return null;

    }

    @Override
    @Nullable
    public List<LSFExClassSet> resolveExParamClasses() {
        return ParamClassesCache.getInstance(getProject()).resolveParamClassesWithCaching(this);
    }

    @Override
    public List<LSFExClassSet> resolveExParamClassesNoCache() {
        LSFPropertyDeclaration decl = getPropertyDeclaration();
        LSFPropertyDeclParams cpd = decl.getPropertyDeclParams();
        List<LSFExClassSet> declareClasses = null;
        if (cpd != null) {
            declareClasses = LSFExClassSet.toEx(LSFPsiImplUtil.resolveExplicitClass(cpd.getClassParamDeclareList()));
            if (LSFPsiUtils.allClassesDeclared(declareClasses)) // оптимизация
                return declareClasses;
        }

        List<LSFExClassSet> valueClasses = resolveValueParamClasses();
        if (valueClasses == null)
            return declareClasses;

        if (declareClasses == null)
            return valueClasses;

        List<LSFExClassSet> mixed = new ArrayList<>(declareClasses);
        for (int i = 0, size = declareClasses.size(); i < size; i++) {
            if (i >= valueClasses.size())
                break;

            if (declareClasses.get(i) == null)
                mixed.set(i, valueClasses.get(i));
        }
        return Collections.unmodifiableList(mixed);
    }

    public static List<LSFClassSet> finishParamClasses(LSFPropDeclaration decl) {
        return LSFExClassSet.fromEx(decl.resolveExParamClasses());
    }

    public static LSFClassSet finishValueClass(LSFPropDeclaration decl) {
        return LSFExClassSet.fromEx(decl.resolveExValueClass(false));
    }
    
    @Override
    public List<LSFClassSet> resolveParamClasses() {
        return finishParamClasses(this);
    }

    @Override
    public LSFClassSet resolveValueClass() {
        return finishValueClass(this);
    }

    @Override
    @Nullable
    public List<LSFExClassSet> inferParamClasses(LSFExClassSet valueClass) {

        List<LSFExClassSet> resultClasses = resolveExParamClasses();
        if (resultClasses == null)
            return null;
        
        resultClasses = new ArrayList<>(resultClasses);

        //        LSFActionStatement action = sourceStatement.getActionPropertyDefinition();
//        return 

        List<LSFExprParamDeclaration> params = null;
        LSFPropertyDeclaration decl = getPropertyDeclaration();
        LSFPropertyDeclParams cpd = decl.getPropertyDeclParams();
        if (cpd != null)
            params = BaseUtils.<List<LSFExprParamDeclaration>>immutableCast(LSFPsiImplUtil.resolveParams(cpd.getClassParamDeclareList()));

        InferExResult inferredClasses = null;
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            LSFExpressionUnfriendlyPD unfriendlyPD = pCalcStatement.getExpressionUnfriendlyPD();
            if (unfriendlyPD != null) {
                PsiElement element = unfriendlyPD.getChildren()[0]; // лень создавать отдельный параметр или интерфейс
                if (element instanceof LSFGroupPropertyDefinition) {
                    List<LSFExClassSet> inferredValueClasses = LSFPsiImplUtil.inferGroupValueParamClasses((LSFGroupPropertyDefinition) element);
                    for (int i = 0; i < resultClasses.size(); i++)
                        if (resultClasses.get(i) == null && i < inferredValueClasses.size()) { // не определены, возьмем выведенные
                            resultClasses.set(i, inferredValueClasses.get(i));
                        }
                    return resultClasses;
                }
            } else {
                LSFPropertyExpression expr = pCalcStatement.getPropertyExpression();
                assert expr != null;
                if (params == null)
                    params = expr.resolveParams();

                inferredClasses = LSFPsiImplUtil.inferParamClasses(expr, valueClass).finishEx();
            }
        } else {
            if(getListActionPropertyDefinitionBody() != null) {
                if (params != null) // может быть action unfriendly
                    inferredClasses = LSFPsiImplUtil.inferActionParamClasses(getListActionPropertyDefinitionBody(), new HashSet<>(params)).finishEx();
            }
        }

        if (inferredClasses != null) {
            assert resultClasses.size() == params.size();
            for (int i = 0; i < resultClasses.size(); i++)
                if (resultClasses.get(i) == null) { // не определены, возьмем выведенные
                    resultClasses.set(i, inferredClasses.get(params.get(i)));
                }
        }

        return resultClasses;
    }

    public boolean isAction() {
        return getActionUnfriendlyPD() != null || getListActionPropertyDefinitionBody() != null;
    }

    public LSFDataPropertyDefinition getDataPropertyDefinition() {
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            LSFExpressionUnfriendlyPD expressionUnfriendlyPD = pCalcStatement.getExpressionUnfriendlyPD();
            if (expressionUnfriendlyPD != null) {
                return expressionUnfriendlyPD.getDataPropertyDefinition();
            }
        }
        return null;
    }

    public boolean isDataProperty() {
        LSFDataPropertyDefinition dataProp = getDataPropertyDefinition();
        return dataProp != null;
    }
    
    public boolean isDataStoredProperty() {
        LSFDataPropertyDefinition dataProp = getDataPropertyDefinition();
        return dataProp != null && dataProp.getDataPropertySessionModifier() == null;
    }

    public boolean isPersistentProperty() {
        LSFNonEmptyPropertyOptions options = getNonEmptyPropertyOptions();
        return options != null && !options.getPersistentSettingList().isEmpty();
    }
    
    public boolean isStoredProperty() {
        return isDataStoredProperty() || isPersistentProperty();
    }

    @Nullable
    @Override
    public Icon getIcon(int flags) {
        return getIcon(getPropType());
    }

    public static Icon getIcon(byte propType) {
        switch (propType) {
            case 3:
                return LSFIcons.ACTION;
            case 2:
                return LSFIcons.ABSTRACT_PROPERTY;
            case 1:
                return LSFIcons.DATA_PROPERTY;
            default:
                return LSFIcons.PROPERTY;
        }
    }

    public byte getPropType() {
        if (isAction()) {
            return 3;
        } else if (isAbstract()) {
            return 2;
        } else if (isDataProperty()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String getPresentableText() {
        return getDeclName() + getParamPresentableText();
    }

    public String getParamPresentableText() {
//        List<LSFStringClassRef> params = getExplicitParams();
        return getParamPresentableText(resolveParamClasses());
    }

    public static String getParamPresentableText(List<?> paramClasses) {
        String result = "";
        if(paramClasses != null) {
            for (Object paramClass : paramClasses)
                result = (result.isEmpty() ? "" : result + ",") + (paramClass != null ? paramClass.toString() : "?");
        } else
            result = "?";
        return "(" + result + ")";
    }

    @NotNull
    public String getValuePresentableText() {
        String valueClassString = "";
        if (!isAction()) {
            LSFClassSet valueClass = resolveValueClass();
            valueClassString = ": " + (valueClass == null ? "?" : valueClass);
        }
        return valueClassString;
    }

    public LSFExplicitClasses getExplicitParams() {
        LSFExplicitSignature declParams = getDeclExplicitParams();
        if (declParams != null && declParams.allClassesDeclared()) // оптимизация
            return declParams;

        LSFExplicitClasses implicitParams = getImplicitExplicitParams();
        if(implicitParams == null)
            return declParams;
        if(declParams == null || !(implicitParams instanceof LSFExplicitSignature)) // если группировка забиваем на explicit
            return implicitParams;

        return declParams.merge((LSFExplicitSignature) implicitParams);
    }

    private LSFExplicitSignature getDeclExplicitParams() {
        List<LSFParamDeclaration> params = getPropertyDeclaration().resolveParamDecls();
        if(params != null)
            return LSFPsiImplUtil.getClassNameRefs(params);
        return null;
    }

    private LSFExplicitClasses getImplicitExplicitParams() {
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            LSFPropertyExpression pExpression = pCalcStatement.getPropertyExpression();
            if (pExpression != null)
                return LSFPsiImplUtil.getClassNameRefs(BaseUtils.<List<LSFParamDeclaration>>immutableCast(pExpression.resolveParams()));

            LSFExpressionUnfriendlyPD unfriend = pCalcStatement.getExpressionUnfriendlyPD();
            if(unfriend != null)
                return LSFPsiImplUtil.getValueParamClassNames(unfriend);
        } else {
            LSFActionUnfriendlyPD unfriend = getActionUnfriendlyPD();
            if (unfriend != null)
                return LSFPsiImplUtil.getValueParamClassNames(unfriend);
        }
        return null;
    }

    public Set<String> getExplicitValues() {
        LSFPropertyCalcStatement pCalcStatement = getPropertyCalcStatement();
        if(pCalcStatement != null) {
            LSFPropertyExpression pExpression = pCalcStatement.getPropertyExpression();
            if (pExpression != null)
                return LSFImplicitExplicitClasses.getNotNullSet(pExpression.getValueClassNames());

            LSFExpressionUnfriendlyPD unfriend = pCalcStatement.getExpressionUnfriendlyPD();
            if(unfriend != null)
                return LSFImplicitExplicitClasses.getNotNullSet(LSFPsiImplUtil.getValueClassNames(unfriend));
        }

        return null;
    }

    @Override
    protected FullNameStubElementType getType() {
        return LSFStubElementTypes.PROP;
    }

    public static boolean resolveEquals(List<LSFClassSet> list1, List<LSFClassSet> list2) {
        if (list1 == null && list2 == null) {
            return true;
        }
        if (list1 == null || list2 == null || list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            if (list1.get(i) == null) {
                if (list2.get(i) != null) {
                    return false;
                }
            } else {
                if (!list1.get(i).equals(list2.get(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public PsiElement[] processImplementationsSearch() {
        LSFId nameIdentifier = getNameIdentifier();
        if (nameIdentifier == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        Collection<PsiReference> refs = ReferencesSearch.search(nameIdentifier, GlobalSearchScope.allScope(getProject())).findAll();

        List<PsiElement> result = new ArrayList<>();
        for (PsiReference ref : refs) {
            LSFOverrideStatement overrideStatement = PsiTreeUtil.getParentOfType((PsiElement) ref, LSFOverrideStatement.class);
            if (overrideStatement != null && ref.equals(overrideStatement.getMappedPropertyClassParamDeclare().getPropertyUsageWrapper().getPropertyUsage())) {
                result.add(overrideStatement.getMappedPropertyClassParamDeclare().getPropertyUsageWrapper());    
            }
        }
        return result.toArray(new PsiElement[result.size()]);
    }

    @Override
    protected Condition<LSFGlobalPropDeclaration> getFindDuplicatesCondition() {
        return new Condition<LSFGlobalPropDeclaration>() {
            @Override
            public boolean value(LSFGlobalPropDeclaration decl) {
                LSFId nameIdentifier = getNameIdentifier();
                LSFId otherNameIdentifier = decl.getNameIdentifier();
                return nameIdentifier != null && otherNameIdentifier != null &&
                       nameIdentifier.getText().equals(otherNameIdentifier.getText()) &&
                       resolveEquals(resolveParamClasses(), decl.resolveParamClasses());
            }
        };
    }
    
    protected Condition<LSFGlobalPropDeclaration> getFindDuplicateColumnsCondition() {
        return new Condition<LSFGlobalPropDeclaration>() {
            @Override
            public boolean value(LSFGlobalPropDeclaration decl) {
                String tableName = getTableName();
                String otherTableName = decl.getTableName();

                if (tableName != null && tableName.equals(otherTableName)) {
                    String columnName = getColumnName();
                    String otherColumnName = decl.getColumnName();

                    return columnName != null && columnName.equals(otherColumnName);
                }
                
                return false;
            }
        };
    }
    
    public boolean resolveDuplicateColumns() {
        CollectionQuery<LSFGlobalPropDeclaration> declarations = new CollectionQuery<>(
                LSFGlobalResolver.findElements(
                        getDeclName(), null, getLSFFile(), getTypes(), getFindDuplicateColumnsCondition(), Finalizer.EMPTY
                )
        );
        return declarations.findAll().size() > 1;
    }

    @Nullable
    @Override
    public String getTableName() {
        return TableNameCache.getInstance(getProject()).getTableNameWithCaching(this);
    }

    @Nullable
    @Override
    public String getTableNameNoCache() {
        if (!isStoredProperty()) {
            return null;
        }
        
        List<LSFClassSet> classesList = resolveParamClasses();
        if (classesList == null) {
            return null;
        }
        
        int paramCount = classesList.size();
        
        if (paramCount == 0) {
            return TABLE_BASE0;
        }

        boolean useBase = false;
        LSFValueClass classes[][] = new LSFValueClass[paramCount][];
        int currentInd[] = new int[paramCount];
        LSFValueClass currentSet[] = new LSFValueClass[paramCount];
        for (int i = 0; i < paramCount; i++) {
            LSFClassSet classSet = classesList.get(i);
            if (classSet == null) {
                useBase = true;
                break;
            } else {
                Collection<LSFValueClass> parents = CustomClassSet.getClassParentsRecursively(classSet.getCommonClass());
                classes[i] = new LSFValueClass[parents.size()];
                int j = 0;
                for (LSFValueClass parent : parents) {
                    classes[i][j++] = parent;
                }
                if (j == 0) {
                    useBase = true;
                    break;
                }
                
                currentSet[i] = classes[i][0];
                currentInd[i] = 0;
            }
        }
        
        if (!useBase) {
            //перебираем возможные классы параметров
            do {
                String tableName = findAppropriateTable(currentSet);
                if (tableName != null) {
                    return tableName;
                }

                int i = paramCount - 1;
                while (i >= 0 && currentInd[i] == classes[i].length - 1) {
                    currentInd[i] = 0;
                    currentSet[i] = classes[i][0];
                    --i;
                }

                if (i < 0) {
                    break;
                }

                currentInd[i]++;
                currentSet[i] = classes[i][currentInd[i]];
            } while (true);
        }

        return TABLE_BASE_PREFIX + paramCount;
    }

    @Nullable
    private String findAppropriateTable(@NotNull LSFValueClass[] currentSet) {
        String names[] = new String[currentSet.length];
        for (int i = 0; i < currentSet.length; i++) {
            names[i] = currentSet[i].getName();
        }

        String key = BaseUtils.toString(names);
        
        Collection<LSFTableDeclaration> tables = TableClassesIndex.getInstance().get(key, getProject(), getLSFFile().getRequireScope());
        for (LSFTableDeclaration table : tables) {
            LSFValueClass[] tableClasses = table.getClasses();

            if (tableClasses.length != currentSet.length) {
                continue;
            }

            boolean fit = true;
            for (int i = 0; i < currentSet.length; ++i) {
                if (tableClasses[i] != currentSet[i]) {
                    fit = false;
                    break;
                }
            }

            if (fit) {
                return table.getNamespaceName() + "_" + table.getName();
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getColumnName() {
        return ColumnNameCache.getInstance(getProject()).getColumnNameWithCaching(this);
    }

    @Nullable
    @Override
    public String getColumnNameNoCache() {
        if (!isStoredProperty()) {
            return null;
        }

        LSFFile lsfFile = getLSFFile();
        ColumnNamingPolicy columnNamingPolicy = ColumnNamingPolicyCache.getInstance(lsfFile).getColumnNamingPolicyWithCaching(lsfFile);
        return columnNamingPolicy == null ? null : columnNamingPolicy.getColumnName(this);
    }

    @Override
    public ElementMigration getMigration(String newName) {
        return PropertyMigration.create(this, getGlobalName(), newName);
    }
    
    @Override
    public Set<LSFPropDeclaration> getDependencies() {
        return getPropDependencies(this);
    }
    
    public static Set<LSFPropDeclaration> getPropDependencies(LSFPropDeclaration prop) {
        Set<LSFPropDeclaration> result = new HashSet<>();

        Collection<LSFPropReference> propReferences;

        if (prop instanceof LSFGlobalPropDeclaration && prop.isAbstract()) {
            Collection<PsiReference> references = ReferencesSearch.search(prop.getNameIdentifier()).findAll();
            propReferences = new ArrayList<>();
            for (PsiReference reference : references) {
                LSFOverrideStatement overrideStatement = PsiTreeUtil.getParentOfType((PsiElement) reference, LSFOverrideStatement.class);
                if (overrideStatement != null && reference.equals(overrideStatement.getMappedPropertyClassParamDeclare().getPropertyUsageWrapper().getPropertyUsage())) {
                    for (LSFPropertyExpression propertyExpression : overrideStatement.getPropertyExpressionList()) {
                        propReferences.addAll(PsiTreeUtil.findChildrenOfType(propertyExpression, LSFPropReference.class));    
                    }
                }    
            }
        } else {
            propReferences = PsiTreeUtil.findChildrenOfType(prop, LSFPropReference.class);
        }

        for (LSFPropReference propReference : propReferences) {
            LSFPropDeclaration propDeclaration = propReference.resolveDecl();
            if (propDeclaration != null) {
                result.add(propDeclaration);
            }
        }

        return result;    
    }

    @Override
    public Set<LSFPropDeclaration> getDependents() {
        return getPropDependents(this);
    }
    
    public static Set<LSFPropDeclaration> getPropDependents(LSFPropDeclaration prop) {
        Set<LSFPropDeclaration> result = new HashSet<>();

        Set<PsiReference> refs = new HashSet<>(ReferencesSearch.search(prop.getNameIdentifier(), prop.getUseScope()).findAll());

        for (PsiReference ref : refs) {
            LSFPropDeclaration dependent = PsiTreeUtil.getParentOfType(ref.getElement(), LSFPropDeclaration.class);
            if (dependent != null) {
                result.add(dependent);
            }
        }

        return result;     
    }

    @Override
    public Integer getComplexity() {
        return getPropComplexity(this);
    }

    public static Integer getPropComplexity(LSFPropDeclaration prop) {
        return getPropComplexity(prop, new HashSet<LSFPropDeclaration>());
    }
    
    private static Integer getPropComplexity(LSFPropDeclaration prop, Set<LSFPropDeclaration> processed) {
        Integer complexity = 1;
        if (!processed.contains(prop)) {
            processed.add(prop);
            if (!(prop instanceof LSFGlobalPropDeclaration && ((LSFGlobalPropDeclaration) prop).isPersistentProperty())) {
                Set<LSFPropDeclaration> dependencies = PropertyDependenciesCache.getInstance(prop.getProject()).resolveWithCaching(prop);
                for (LSFPropDeclaration dependency : dependencies) {
                    complexity += getPropComplexity(dependency, processed);
                }
            }
        }

        return complexity;    
    }

    @Override
    public Icon getIcon() {
        return getIcon(0);
    }
}

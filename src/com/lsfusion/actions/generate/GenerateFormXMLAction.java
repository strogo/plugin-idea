package com.lsfusion.actions.generate;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GenerateFormXMLAction extends GenerateFormAction {

    protected Object getRootElement(AnActionEvent e) throws JDOMException, IOException {
        final FileChooserDescriptor fileChooser = FileChooserDescriptorFactory.createSingleFileDescriptor("xml");
        VirtualFile file = FileChooser.chooseFile(fileChooser, e.getProject(), null);
        return file != null ? new SAXBuilder().build(new File(file.getPath())).getRootElement() : null;
    }

    @Override
    protected List<ParseNode> generateHierarchy(Object element, String key) {
        return generateHierarchyXML((Element) element, key);
    }

    private List<ParseNode> generateHierarchyXML(Element element, String key) {
        List<ParseNode> children = new ArrayList<>();

        ElementNamespace namespace = getElementNamespace(element);

        boolean noChildren = element.getChildren().isEmpty();
        boolean noAttributes = element.getAttributes().isEmpty();
        boolean isPropertyParseNode = noChildren && noAttributes;

        if (isPropertyParseNode) {
            return Collections.singletonList(new PropertyParseNode(key, namespace, false));
        } else {

            Map<String, List<Element>> childrenMap = getChildrenMap(element);

            for (Map.Entry<String, List<Element>> childEntry : childrenMap.entrySet()) {
                String childKey = childEntry.getKey();
                List<Element> childrenList = childEntry.getValue();

                boolean isGroupObjectParseNode = childrenList.size() > 1;

                if (isGroupObjectParseNode) {

                    List<ParseNode> localChildren = new ArrayList<>();
                    for (Element c : childrenList) {
                        localChildren.addAll(generateHierarchyXML(c, childKey));
                    }

                    ParseNode mergedChild = deepMerge(localChildren);
                    boolean integrationKey = mergedChild.children.size() > 0;

                    List<ParseNode> nChildren = new ArrayList<>();
                    nChildren.add(mergedChild);

                    children.add(new GroupObjectParseNode(childKey, nChildren, namespace, integrationKey));

                } else { //propertyGroupParseNode, childrenList.size() == 1

                    for (Element child : childrenList) {
                        children.addAll(generateHierarchyXML(child, childKey));
                    }
                }
            }

            for (Attribute attribute : element.getAttributes()) {
                children.add(new PropertyParseNode(attribute.getName(), namespace, true));
            }

            List<ParseNode> result = new ArrayList<>();
            if(noChildren) { //assert !noAttributes
                result.add(new PropertyParseNode(key, namespace, false));
            }
            result.add(new PropertyGroupParseNode(key, children, namespace, key == null ? element.getName() : null));
            return result;
        }
    }

    private Map<String, List<Element>> getChildrenMap(Element element) {
        Map<String, List<Element>> childrenMap = new LinkedHashMap<>();
        for(Element child : element.getChildren()) {
            List<Element> childrenList = childrenMap.get(child.getName());
            if(childrenList == null)
                childrenList = new ArrayList<>();
            childrenList.add(child);
            childrenMap.put(child.getName(), childrenList);
        }
        return childrenMap;
    }

    private ElementNamespace getElementNamespace(Element element) {
        String prefix = element.getNamespacePrefix();
        String uri = element.getNamespaceURI();
        return !prefix.isEmpty() ? new ElementNamespace(prefix, uri) : null;
    }
}

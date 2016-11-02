package com.lsfusion.dependencies.property;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.util.Function;
import com.lsfusion.actions.ToggleComplexityAction;
import com.lsfusion.lang.psi.LSFPropertyStatement;
import com.lsfusion.lang.psi.cache.PropertyComplexityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PropertyComplexityLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        if (!elements.isEmpty() && !ToggleComplexityAction.isComplexityEnabled(elements.iterator().next().getProject())) {
            return;
        }
        
        Document document = null;
        Set<Integer> usedLines = new HashSet<>();
        for (PsiElement element : elements) {
            if (document == null) {
                document = PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());
            }
            if (element instanceof LSFPropertyStatement && ((LSFPropertyStatement) element).isCorrect() && !((LSFPropertyStatement) element).isAction() && !((LSFPropertyStatement) element).isStoredProperty()) {
                int lineNumber = document.getLineNumber(element.getTextOffset());
                if (!usedLines.contains(lineNumber)) {
                    result.add(createLineMarker((LSFPropertyStatement) element));
                    usedLines.add(lineNumber);
                }
            }
        }
    }

    private LineMarkerInfo createLineMarker(LSFPropertyStatement psi) {
        int complexity = PropertyComplexityCache.getInstance(psi.getProject()).resolveWithCaching(psi); 
        return new LineMarkerInfo(
                psi,
                psi.getTextRange(),
                createIcon(complexity),
                Pass.UPDATE_OVERRIDDEN_MARKERS,
                PropertyComplexityTooltipProvider.INSTANCE,
                null,
                GutterIconRenderer.Alignment.RIGHT
        );
    }

    private static Icon createIcon(final int complexity) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                String text = "" + complexity;
                g.setColor(new JBColor(new Color(114, 174, 108), new Color(99, 146, 97)));

                g.drawRoundRect(x, y, 12, 12, 2, 2);
                Font textFont = new Font("Dialog", Font.BOLD, 11);
                g.setFont(textFont);

                int textWidth = g.getFontMetrics(textFont).stringWidth(text);
                g.drawString(text, x + (12 - textWidth) / 2, y + 11);
            }

            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }
        };
    }

    private static class PropertyComplexityTooltipProvider implements Function<PsiElement, String> {
        static PropertyComplexityTooltipProvider INSTANCE = new PropertyComplexityTooltipProvider();

        @Override
        public String fun(PsiElement psi) {
            return "Property complexity";
        }
    }
}

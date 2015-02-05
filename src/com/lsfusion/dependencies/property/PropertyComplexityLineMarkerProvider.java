package com.lsfusion.dependencies.property;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.util.Function;
import com.lsfusion.lang.psi.LSFPropertyStatement;
import com.lsfusion.lang.psi.cache.PropertyComplexityCache;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

public class PropertyComplexityLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(List<PsiElement> elements, Collection<LineMarkerInfo> result) {
        for (PsiElement element : elements) {
            if (element instanceof LSFPropertyStatement && !((LSFPropertyStatement) element).isAction() && !((LSFPropertyStatement) element).isStoredProperty()) {
                result.add(createLineMarker((LSFPropertyStatement) element));
            }
        }
    }

    private LineMarkerInfo createLineMarker(LSFPropertyStatement psi) {
        int complexity = PropertyComplexityCache.getInstance(psi.getProject()).resolveWithCaching(psi); 
        return new LineMarkerInfo(
                psi,
                psi.getTextRange().getStartOffset(),
                createIcon(complexity),
                Pass.UPDATE_OVERRIDEN_MARKERS,
                PropertyComplexityTooltipProvider.INSTANCE,
                null
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
package stimulus.lang;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment;
import com.intellij.lang.javascript.psi.JSField;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigLanguage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stimulus.lang.twig.StimulusTwigPatterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static stimulus.lang.js.StimulusJSReferenceContributorKt.targetsField;
import static stimulus.lang.js.StimulusJSReferenceContributorKt.valuesField;

public class StimulusGoToDeclarationHandler implements GotoDeclarationHandler {
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if (!PlatformPatterns.psiElement().withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {
            return null;
        }

        Collection<PsiElement> targets = new ArrayList<>();

        if (StimulusTwigPatterns.Companion.getPrintBlockOrTagFunctionPattern("stimulus_controller").accepts(psiElement)) {
            targets.add(StimulusReferencesKt.resolveController(psiElement.getText(), psiElement));
        }
        if (StimulusTwigPatterns.Companion.getPrintBlockOrTagFunctionPattern("stimulus_action").accepts(psiElement)) {
            targets.add(StimulusReferencesKt.resolveController(psiElement.getText(), psiElement));
        }
        if (StimulusTwigPatterns.Companion.getPrintBlockOrTagFunctionPattern("stimulus_target").accepts(psiElement)) {
            targets.add(StimulusReferencesKt.resolveController(psiElement.getText(), psiElement));
        }

        if (StimulusTwigPatterns.Companion.getFunctionWithSecondParameterAsKeyLiteralPattern("stimulus_controller").accepts(psiElement)) {
            Pattern regexControllerName = Pattern.compile("stimulus_controller\\(\\s?['\"]+\\s?([^'\"]+)\\s?['\"]+\\s?[^\\)]*\\)");
            Matcher matcherControllerName = regexControllerName.matcher(psiElement.getParent().getParent().getText());

            if (matcherControllerName.find()) {
                String controllerName = matcherControllerName.group(1);
                ES6ExportDefaultAssignment controller = (ES6ExportDefaultAssignment) StimulusReferencesKt.resolveController(controllerName, psiElement);
                if (controller != null) {
                    JSClass jsController = (JSClass) controller.getNamedElement();
                    JSField field = jsController.findFieldByName(valuesField);
                    if (field != null) {
                        targets.add(field);
                    }
                }
            }
        }

        if (StimulusTwigPatterns.Companion.getFunctionSecondParameter("stimulus_action").accepts(psiElement)) {
            Pattern regexControllerName = Pattern.compile("stimulus_action\\(\\s?['\"]+\\s?([^'\"]+)\\s?['\"]+\\s?[^\\)]*\\)");
            Matcher matcherControllerName = regexControllerName.matcher(psiElement.getParent().getText());

            if (matcherControllerName.find()) {
                String controllerName = matcherControllerName.group(1);
                ES6ExportDefaultAssignment controller = (ES6ExportDefaultAssignment) StimulusReferencesKt.resolveController(controllerName, psiElement);
                if (controller != null) {
                    JSClass jsController = (JSClass) controller.getNamedElement();
                    JSFunction function = jsController.findFunctionByName(psiElement.getText());
                    if (function != null) {
                        targets.add(function);
                    }
                }
            }
        }

        if (StimulusTwigPatterns.Companion.getFunctionSecondParameter("stimulus_target").accepts(psiElement)) {
            Pattern regexControllerName = Pattern.compile("stimulus_target\\(\\s?['\"]+\\s?([^'\"]+)\\s?['\"]+\\s?[^\\)]*\\)");
            Matcher matcherControllerName = regexControllerName.matcher(psiElement.getParent().getText());

            if (matcherControllerName.find()) {
                String controllerName = matcherControllerName.group(1);
                ES6ExportDefaultAssignment controller = (ES6ExportDefaultAssignment) StimulusReferencesKt.resolveController(controllerName, psiElement);
                if (controller != null) {
                    JSClass jsController = (JSClass) controller.getNamedElement();
                    JSField field = jsController.findFieldByName(targetsField);
                    if (field != null) {
                        targets.add(field);
                    }
                }
            }
        }


        return targets.toArray(new PsiElement[0]);
    }


    @Override
    public @Nullable @Nls(capitalization = Nls.Capitalization.Title) String getActionText(@NotNull DataContext context) {
        return GotoDeclarationHandler.super.getActionText(context);
    }
}

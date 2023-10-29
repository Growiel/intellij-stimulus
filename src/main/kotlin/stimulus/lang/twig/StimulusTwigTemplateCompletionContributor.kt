package stimulus.lang.twig

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.ProcessingContext
import com.jetbrains.twig.TwigLanguage
import com.jetbrains.twig.TwigTokenTypes
import com.jetbrains.twig.elements.TwigCompositeElement
import com.jetbrains.twig.elements.TwigElementTypes
import stimulus.lang.getAllControllers
import stimulus.lang.getLiteralValues
import stimulus.lang.js.targetPropertySuffix
import stimulus.lang.js.targetsField
import stimulus.lang.js.valuesField
import stimulus.lang.resolveController
import stimulus.lang.toControllerName

class StimulusTwigTemplateCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, StimulusTwigPatterns.getPrintBlockOrTagFunctionPattern("stimulus_controller"), ControllerNameProvider())
        extend(CompletionType.BASIC, StimulusTwigPatterns.getFunctionWithSecondParameterAsKeyLiteralPattern("stimulus_controller"), ControllerValuesProvider())

        extend(CompletionType.BASIC, StimulusTwigPatterns.getPrintBlockOrTagFunctionPattern("stimulus_target"), ControllerNameProvider())
        extend(CompletionType.BASIC, StimulusTwigPatterns.getFunctionSecondParameter("stimulus_target"), ControllerTargetsProvider())

        extend(CompletionType.BASIC, StimulusTwigPatterns.getPrintBlockOrTagFunctionPattern("stimulus_action"), ControllerNameProvider())
        extend(CompletionType.BASIC, StimulusTwigPatterns.getFunctionSecondParameter("stimulus_action"), ControllerActionsProvider())
//        extend(CompletionType.BASIC, getFunctionThirdParameter("stimulus_action"), ControllerEventsProvider())
    }
}

abstract class StimulusCompletionProvider : CompletionProvider<CompletionParameters>() {
    fun getControllerFile(regex: Regex, element: PsiElement, functionCallElement: PsiElement) : JSClass? {
        val matchControllerName = regex.find(functionCallElement.text)!!
        val controllerName = matchControllerName.groups[1]?.value ?: return null

        var controllerFile = (resolveController(controllerName, element) as? ES6ExportDefaultAssignment)
        if (controllerFile != null) {
            return controllerFile.namedElement as? JSClass
        }

        return null;
    }
}

class ControllerNameProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val parent = parameters.position.parent
        if (
            parent is TwigCompositeElement
        ) {
            result.addAllElements(
                getAllControllers(parameters.position)
                    .map { toControllerName(it) }
                    .map { LookupElementBuilder.create(it) }
            )
        }
    }
}

class ControllerValuesProvider : StimulusCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val position = parameters.position
        val functionCallElement = position.parent.parent

        // Controller values
        if (
            functionCallElement is TwigCompositeElement
        ) {
            // Figure out the controller, so we extract the values from it.
            val regexControllerName = """stimulus_controller\(\s?['\"]+\s?([^'\"]+)\s?['\"]+\s?[^\)]*\)""".toRegex()
            val controller = this.getControllerFile(regexControllerName, position, functionCallElement) ?: return

            // Find out the values that are already used, so we don't re-recommend them.
            val regexValues = """\s*?['"]\s*?([^'"]+)\s*?['"]\s*:""".toRegex()
            val alreadyUsedValues = regexValues.findAll(position.parent.text)
                .map { it.groupValues[1] }
                .filter { !it.contains("IntellijIdeaRulezzz") }

            result.addAllElements((controller.findFieldByName(valuesField)
                ?.initializer as? JSObjectLiteralExpression)
                ?.properties
                ?.filter { it.name != null }
                ?.filter { !alreadyUsedValues.contains(it.name) }
                ?.map {
                    LookupElementBuilder.create((it.name as String)).withTypeText(it.jsType?.typeText?.replace("#referenceType(", "")?.replace(")", ""))
                } ?: emptyList())
        }
    }
}

class ControllerTargetsProvider : StimulusCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val position = parameters.position
        val functionCallElement = position.parent

        if (
            functionCallElement is TwigCompositeElement
        ) {
            // Figure out the controller, so we extract the values from it.
            val regexControllerName = """stimulus_target\(\s?['\"]+\s?([^'\"]+)\s?['\"]+\s?[^\)]*\)""".toRegex()
            val controller = this.getControllerFile(regexControllerName, position, functionCallElement) ?: return

            result.addAllElements(getLiteralValues(controller.findFieldByName(targetsField)).map {
                LookupElementBuilder.create(it)
            })
        }
    }
}

class ControllerActionsProvider : StimulusCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val position = parameters.position
        val functionCallElement = position.parent

        if (
            functionCallElement is TwigCompositeElement
        ) {
            // Figure out the controller, so we extract the values from it.
            val regexControllerName = """stimulus_action\(\s?['\"]+\s?([^'\"]+)\s?['\"]+\s?[^\)]*\)""".toRegex()
            val controller = this.getControllerFile(regexControllerName, position, functionCallElement) ?: return

            result.addAllElements(controller.functions.map { LookupElementBuilder.create(it) })
        }
    }
}

// Too complex for now, we'd need to parse the file to lookup calls do dispatch. Most of the time we're using basic JS events anyways.
class ControllerEventsProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val position = parameters.position
        val functionCallElement = position.parent.parent

        if (
            functionCallElement is TwigCompositeElement
        ) {
            val a = 'a'
        }
    }
}

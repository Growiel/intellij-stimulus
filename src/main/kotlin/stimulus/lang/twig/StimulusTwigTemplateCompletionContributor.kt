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
    private val PARAMETER_WHITE_LIST = arrayOf<ElementPattern<PsiElement>>(
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.NUMBER),
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.CONCAT),
        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER),
        PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
        PlatformPatterns.psiElement(TwigTokenTypes.DOT)
    )

    init {
        extend(CompletionType.BASIC, getPrintBlockOrTagFunctionPattern("stimulus_controller"), ControllerNameProvider())
        extend(CompletionType.BASIC, getFunctionWithSecondParameterAsKeyLiteralPattern("stimulus_controller"), ControllerValuesProvider())

        extend(CompletionType.BASIC, getPrintBlockOrTagFunctionPattern("stimulus_target"), ControllerNameProvider())
        extend(CompletionType.BASIC, getFunctionSecondParameter("stimulus_target"), ControllerTargetsProvider())

        extend(CompletionType.BASIC, getPrintBlockOrTagFunctionPattern("stimulus_action"), ControllerNameProvider())
        extend(CompletionType.BASIC, getFunctionSecondParameter("stimulus_action"), ControllerActionsProvider())
//        extend(CompletionType.BASIC, getFunctionThirdParameter("stimulus_action"), ControllerEventsProvider())
    }

    // Copied from https://github.com/Haehnchen/idea-php-symfony2-plugin/blob/ff5c0c88dd4310fc1da514eae67427e4f77d0bed/src/main/java/fr/adrienbrault/idea/symfony2plugin/templating/TwigPattern.java
    private fun getPrintBlockOrTagFunctionPattern(vararg functionName: String?): PsiElementPattern.Capture<out PsiElement> {
        return (
            (
                PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                    .withParent(getFunctionCallScopePattern()) as PsiElementPattern.Capture<*>
            ).afterLeafSkipping(
                PlatformPatterns.or(
                    *arrayOf<ElementPattern<*>>(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace::class.java),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    )
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(
                    PlatformPatterns.string().oneOf(*functionName)
                )
            ) as PsiElementPattern.Capture<*>
        ).withLanguage(TwigLanguage.INSTANCE)
    }

    // Copied from https://github.com/Haehnchen/idea-php-symfony2-plugin/blob/ff5c0c88dd4310fc1da514eae67427e4f77d0bed/src/main/java/fr/adrienbrault/idea/symfony2plugin/templating/TwigPattern.java
    private fun getFunctionCallScopePattern(): ElementPattern<PsiElement> {
        val var10000: ElementPattern<PsiElement> = PlatformPatterns.or(*arrayOf<ElementPattern<PsiElement>>(PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK), PlatformPatterns.psiElement(TwigElementTypes.TAG), PlatformPatterns.psiElement(TwigElementTypes.IF_TAG), PlatformPatterns.psiElement(TwigElementTypes.SET_TAG), PlatformPatterns.psiElement(TwigElementTypes.ELSE_TAG), PlatformPatterns.psiElement(TwigElementTypes.ELSEIF_TAG), PlatformPatterns.psiElement(TwigElementTypes.FOR_TAG), PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL)))
        return var10000
    }

    fun getFunctionSecondParameter(vararg functionName: String?): ElementPattern<PsiElement> {
        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace::class.java),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                    PlatformPatterns.or<Any>(*PARAMETER_WHITE_LIST),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                        PlatformPatterns.or<PsiElement>(
                            PlatformPatterns.psiElement<PsiWhiteSpace>(PsiWhiteSpace::class.java),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(TwigTokenTypes.NUMBER)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(*functionName))
                    )
                )
            ).withLanguage(TwigLanguage.INSTANCE)
    }

    fun getFunctionThirdParameter(vararg functionName: String?): ElementPattern<PsiElement> {
        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace::class.java),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                PlatformPatterns.or<Any>(
                    *PARAMETER_WHITE_LIST,
                    PlatformPatterns.psiElement(PsiWhiteSpace::class.java)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                    PlatformPatterns.or<Any>(
                        *PARAMETER_WHITE_LIST,
                        PlatformPatterns.psiElement(PsiWhiteSpace::class.java)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                        PlatformPatterns.or<PsiElement>(
                            PlatformPatterns.psiElement<PsiWhiteSpace>(PsiWhiteSpace::class.java),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(TwigTokenTypes.NUMBER)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(*functionName))
                    )
                )
            ),
        ).withLanguage(TwigLanguage.INSTANCE)
    }

    // Copied from https://github.com/Haehnchen/idea-php-symfony2-plugin/blob/ff5c0c88dd4310fc1da514eae67427e4f77d0bed/src/main/java/fr/adrienbrault/idea/symfony2plugin/templating/TwigPattern.java
    fun getFunctionWithSecondParameterAsKeyLiteralPattern(vararg functionName: String?): ElementPattern<PsiElement> {
        val parameterPattern = PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
            PlatformPatterns.or<PsiElement>(
                PlatformPatterns.psiElement<PsiWhiteSpace>(PsiWhiteSpace::class.java),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                PlatformPatterns.or<Any>(*PARAMETER_WHITE_LIST),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                    PlatformPatterns.or<PsiElement>(
                        PlatformPatterns.psiElement<PsiWhiteSpace>(PsiWhiteSpace::class.java),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.NUMBER)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(*functionName))
                )
            )
        )

        return PlatformPatterns.or( // {{ foo({'foobar': 'foo', 'foo<caret>bar': 'foo'}}) }}
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace::class.java),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).withParent(parameterPattern)
                ).withLanguage(TwigLanguage.INSTANCE),  // {{ foo(12, {'foo<caret>bar': 'foo'}}) }}
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace::class.java),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL).withParent(parameterPattern)
                )
                .withLanguage(TwigLanguage.INSTANCE)
        )
    }
}

abstract class StimulusCompletionProvider : CompletionProvider<CompletionParameters>() {
    fun getControllerFile(regex: Regex, element: PsiElement, functionCallElement: PsiElement) : JSClass? {
        val matchControllerName = regex.find(functionCallElement.text)!!
        val controllerName = matchControllerName.groups[1]?.value ?: return null

        // Try the lower case and upper case version of the file.
        var controllerFile = (resolveController(controllerName, element) as? ES6ExportDefaultAssignment)
        if (controllerFile == null) {
            controllerFile = (resolveController(controllerName.replaceFirstChar { it.uppercase() }, element) as? ES6ExportDefaultAssignment) ?: return null
        }
        return controllerFile.namedElement as? JSClass
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

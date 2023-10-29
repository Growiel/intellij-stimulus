package stimulus.lang.twig

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.twig.TwigLanguage
import com.jetbrains.twig.TwigTokenTypes
import com.jetbrains.twig.elements.TwigElementTypes

class StimulusTwigPatterns {
    companion object {
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

        fun getPrintBlockOrTagFunctionPattern(vararg functionName: String?): PsiElementPattern.Capture<out PsiElement> {
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
        fun getFunctionCallScopePattern(): ElementPattern<PsiElement> {
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
}

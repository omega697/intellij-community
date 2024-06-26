// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfTypes
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.codeinsights.impl.base.inspections.getScopeToSearchParameterReferences
import org.jetbrains.kotlin.idea.quickfix.RemoveValVarFromParameterFix
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.util.match

internal val CONSTRUCTOR_VAL_VAR_MODIFIERS = listOf(
    OPEN_KEYWORD, FINAL_KEYWORD, OVERRIDE_KEYWORD,
    PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD,
    LATEINIT_KEYWORD
)


class CanBeParameterInspection : AbstractKotlinInspection() {
    private fun PsiReference.usedAsPropertyIn(klass: KtClass): Boolean {
        if (this !is KtSimpleNameReference) return true
        val nameExpression = element
        // receiver.x
        val parent = element.parent
        if (parent is KtQualifiedExpression) {
            if (parent.selectorExpression == element) return true
        }
        // x += something
        if (parent is KtBinaryExpression &&
            parent.left == element &&
            KtPsiUtil.isAssignment(parent)
        ) return true
        // init / constructor / non-local property?
        var parameterUser: PsiElement = nameExpression
        do {
            parameterUser = parameterUser.parentOfTypes(
                KtProperty::class,
                KtPropertyAccessor::class,
                KtClassInitializer::class,
                KtFunction::class,
                KtObjectDeclaration::class,
                KtSuperTypeCallEntry::class
            ) ?: return true
        } while (parameterUser is KtProperty && parameterUser.isLocal)
        return when (parameterUser) {
            is KtProperty -> parameterUser.containingClassOrObject !== klass
            is KtClassInitializer -> parameterUser.containingDeclaration !== klass
            is KtFunction, is KtObjectDeclaration, is KtPropertyAccessor -> true
            is KtSuperTypeCallEntry -> parameterUser.getStrictParentOfType<KtClassOrObject>() !== klass
            else -> true
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return parameterVisitor(fun(parameter: KtParameter) {
            // Applicable to val / var parameters of a class / object primary constructors
            val valOrVar = parameter.valOrVarKeyword ?: return
            if (parameter.hasModifier(OVERRIDE_KEYWORD) || parameter.hasModifier(ACTUAL_KEYWORD)) return
            if (parameter.annotationEntries.isNotEmpty()) return
            val constructor = parameter.parents.match(KtParameterList::class, last = KtPrimaryConstructor::class) ?: return
            val klass = constructor.getContainingClassOrObject() as? KtClass ?: return
            if (klass.isData()) return

            val scopeToSearch = getScopeToSearchParameterReferences(parameter) ?: return
            // Find all references and check them
            val references = ReferencesSearch.search(parameter, scopeToSearch).findAll()
            if (references.isEmpty()) return
            if (references.any { it.element.parent is KtCallableReferenceExpression || it.usedAsPropertyIn(klass) }) return
            holder.registerProblem(
                valOrVar,
                KotlinBundle.message("constructor.parameter.is.never.used.as.a.property"),
                RemoveValVarFix(parameter)
            )
        })
    }

    class RemoveValVarFix(private val fix : RemoveValVarFromParameterFix) : LocalQuickFix {

        constructor(parameter: KtParameter): this(RemoveValVarFromParameterFix(parameter))

        override fun getName() = fix.text

        override fun getFamilyName() = fix.familyName

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val parameter = descriptor.psiElement.getParentOfType<KtParameter>(strict = true) ?: return
            parameter.valOrVarKeyword?.delete()
            // Delete visibility / open / final / lateinit, if any
            // Retain vararg
            // override should never be here
            val modifierList = parameter.modifierList ?: return
            for (modifier in CONSTRUCTOR_VAL_VAR_MODIFIERS) {
                modifierList.getModifier(modifier)?.delete()
            }
        }

        override fun getFileModifierForPreview(target: PsiFile): FileModifier? {
            val newFix = fix.getFileModifierForPreview(target) as? RemoveValVarFromParameterFix
            return newFix?.let(::RemoveValVarFix)
        }
    }
}


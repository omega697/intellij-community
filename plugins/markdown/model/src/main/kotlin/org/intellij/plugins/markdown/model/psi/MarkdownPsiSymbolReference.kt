package org.intellij.plugins.markdown.model.psi

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface MarkdownPsiSymbolReference: PsiSymbolReference {
  companion object {
    fun findSymbolReferences(element: PsiElement): Sequence<MarkdownPsiSymbolReference> {
      val service = PsiSymbolReferenceService.getService()
      val references = service.getReferences(element).asSequence()
      return references.filterIsInstance<MarkdownPsiSymbolReference>()
    }
  }
}

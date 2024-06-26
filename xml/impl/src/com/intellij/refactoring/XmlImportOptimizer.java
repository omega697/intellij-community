// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.refactoring;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.analysis.XmlUnusedNamespaceInspection;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.QuickFix;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.XmlBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public class XmlImportOptimizer implements ImportOptimizer {

  private final XmlUnusedNamespaceInspection myInspection = new XmlUnusedNamespaceInspection();
  private final Condition<ProblemDescriptor> myCondition = descriptor -> {
    PsiElement element = descriptor.getPsiElement();
    PsiElement parent = element.getParent();
    return parent != null && !myInspection.isSuppressedFor(parent);
  };

  @Override
  public boolean supports(@NotNull PsiFile file) {
    return file instanceof XmlFile;
  }

  @Override
  public @NotNull CollectingInfoRunnable processFile(final @NotNull PsiFile file) {
    XmlFile xmlFile = (XmlFile)file;
    Project project = xmlFile.getProject();
    HighlightDisplayKey key = HighlightDisplayKey.find(myInspection.getShortName());
    Map<XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix, ProblemDescriptor> fixes = new LinkedHashMap<>();
    if (InspectionProjectProfileManager.getInstance(project).getCurrentProfile().isToolEnabled(key, xmlFile)) {
      ProblemsHolder holder = new ProblemsHolder(InspectionManager.getInstance(project), xmlFile, false);
      final XmlElementVisitor visitor = (XmlElementVisitor)myInspection.buildVisitor(holder, false);
      new PsiRecursiveElementVisitor() {
        @Override
        public void visitElement(@NotNull PsiElement element) {
          if (element instanceof XmlAttribute) {
            visitor.visitXmlAttribute((XmlAttribute)element);
          }
          else {
            super.visitElement(element);
          }
        }
      }.visitFile(xmlFile);
      ProblemDescriptor[] results = holder.getResultsArray();
      List<ProblemDescriptor> list = ContainerUtil.filter(results, myCondition);

      for (ProblemDescriptor result : list) {
        for (QuickFix fix : result.getFixes()) {
          if (fix instanceof XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix) {
            fixes.put((XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix)fix, result);
          }
        }
      }
    }
    return new CollectingInfoRunnable() {
      int myRemovedNameSpaces = 0;

      @Override
      public void run() {
        SmartPsiElementPointer<XmlTag> pointer = null;
        for (Map.Entry<XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix, ProblemDescriptor> fix : fixes.entrySet()) {
          pointer = fix.getKey().doFix(project, fix.getValue(), false);
          myRemovedNameSpaces++;
        }
        if (pointer != null) {
          XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix.reformatStartTag(project, pointer);
        }
      }

      @Override
      public @Nullable String getUserNotificationInfo() {
        return myRemovedNameSpaces > 0
               ? XmlBundle.message("hint.text.removed.namespace", myRemovedNameSpaces, myRemovedNameSpaces > 1 ? 0 : 1)
               : null;
      }
    };
  }
}

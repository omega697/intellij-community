// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.rt.execution.junit;

public interface FileComparisonData {
  String getActualStringPresentation();
  String getExpectedStringPresentation();
  String getFilePath();
  String getActualFilePath();
}

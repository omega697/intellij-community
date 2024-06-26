// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.formatter


import com.intellij.formatting.Alignment
import com.intellij.lang.ASTNode

abstract class CommonAlignmentStrategy {
    abstract fun getAlignment(node: ASTNode): Alignment?
}
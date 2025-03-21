// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.application.impl

import com.intellij.openapi.application.CoroutineSupport
import org.jetbrains.annotations.ApiStatus.Internal
import kotlin.coroutines.CoroutineContext

@Internal
internal class PlatformCoroutineSupport : CoroutineSupport {

  override fun edtDispatcher(): CoroutineContext {
    return EdtCoroutineDispatcher.Locking
  }

  override fun uiDispatcher(): CoroutineContext {
    return EdtCoroutineDispatcher.NonLocking
  }

  override fun immediateEdtDispatcher(): CoroutineContext {
    return EdtCoroutineDispatcher.Locking.immediate
  }

  override fun immediateUiDispatcher(): CoroutineContext {
    return EdtCoroutineDispatcher.NonLocking.immediate
  }
}

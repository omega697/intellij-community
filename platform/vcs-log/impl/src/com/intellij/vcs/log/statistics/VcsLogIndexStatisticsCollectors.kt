// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.vcs.log.statistics

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.data.VcsLogData
import com.intellij.vcs.log.data.index.VcsLogBigRepositoriesList
import com.intellij.vcs.log.impl.VcsLogSharedSettings
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NonNls
import java.util.concurrent.TimeUnit

@NonNls
internal class VcsLogIndexApplicationStatisticsCollector : ApplicationUsagesCollector() {

  private val GROUP = EventLogGroup("vcs.log.index.application", 3)
  private val INDEX_DISABLED_IN_REGISTRY = GROUP.registerEvent("index.disabled.in.registry", EventFields.Boolean("value"))
  private val INDEX_FORCED_IN_REGISTRY = GROUP.registerEvent("index.forced.in.registry", EventFields.Boolean("value"))
  private val BIG_REPOSITORIES = GROUP.registerEvent("big.repositories", EventFields.Count)

  override fun getMetrics(): MutableSet<MetricEvent> {
    val metricEvents = mutableSetOf<MetricEvent>()
    if (!VcsLogData.isIndexSwitchedOnInRegistry()) {
      metricEvents.add(INDEX_DISABLED_IN_REGISTRY.metric(true))
    }

    if (Registry.`is`("vcs.log.index.force")) {
      metricEvents.add(INDEX_FORCED_IN_REGISTRY.metric(true))
    }

    getBigRepositoriesList()?.let { bigRepositoriesList ->
      if (bigRepositoriesList.repositoryCount > 0) {
        metricEvents.add(BIG_REPOSITORIES.metric(bigRepositoriesList.repositoryCount))
      }
    }

    return metricEvents
  }

  private fun getBigRepositoriesList() = serviceIfCreated<VcsLogBigRepositoriesList>()

  override fun getGroup(): EventLogGroup {
    return GROUP
  }
}

internal class VcsLogIndexProjectStatisticsCollector : ProjectUsagesCollector() {
  private val GROUP = EventLogGroup("vcs.log.index.project", 5)
  private val INDEXING_TIME = GROUP.registerEvent("indexing.time.minutes", EventFields.Count)
  private val IS_PAUSED = EventFields.Boolean("is_paused")
  private val INDEXING_TIME_BY_ROOT = GROUP.registerEvent("indexing.time.by.root",
                                                          EventFields.AnonymizedPath, EventFields.DurationMs, IS_PAUSED)
  private val INDEX_DISABLED = GROUP.registerEvent("index.disabled.in.project", EventFields.Boolean("value"))

  override fun getMetrics(project: Project): Set<MetricEvent> {
    if (!TrustedProjects.isProjectTrusted(project)) return emptySet()

    val usages = mutableSetOf<MetricEvent>()

    getIndexCollector(project)?.state?.let { indexCollectorState ->
      val indexingTime = TimeUnit.MILLISECONDS.toMinutes(indexCollectorState.indexTime).toInt()
      usages.add(INDEXING_TIME.metric(indexingTime))
      for ((rootPath, time) in indexCollectorState.indexTimeByRoot) {
        usages.add(INDEXING_TIME_BY_ROOT.metric(rootPath, time, VcsLogBigRepositoriesList.getInstance().isBig(rootPath)))
      }
    }

    getSharedSettings(project)?.let { sharedSettings ->
      if (!sharedSettings.isIndexSwitchedOn) {
        usages.add(INDEX_DISABLED.metric(true))
      }
    }

    return usages
  }

  private fun getSharedSettings(project: Project) = project.serviceIfCreated<VcsLogSharedSettings>()

  private fun getIndexCollector(project: Project) = project.serviceIfCreated<VcsLogIndexCollector>()

  override fun getGroup(): EventLogGroup {
    return GROUP
  }

}

@ApiStatus.Internal
class VcsLogIndexCollectorState : BaseState() {
  var indexTime by property(0L)
  var indexTimeByRoot by linkedMap<String, Long>()

  fun copy(): VcsLogIndexCollectorState {
    val copy = VcsLogIndexCollectorState()
    copy.indexTime = indexTime
    copy.indexTimeByRoot = indexTimeByRoot.toMap(linkedMapOf())
    return copy
  }
}

@State(name = "VcsLogIndexCollector",
       storages = [Storage(value = StoragePathMacros.CACHE_FILE)])
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class VcsLogIndexCollector : PersistentStateComponent<VcsLogIndexCollectorState> {
  private val lock = Any()
  private var state: VcsLogIndexCollectorState

  init {
    synchronized(lock) {
      state = VcsLogIndexCollectorState()
    }
  }

  override fun getState(): VcsLogIndexCollectorState {
    synchronized(lock) {
      return state.copy()
    }
  }

  override fun loadState(state: VcsLogIndexCollectorState) {
    synchronized(lock) {
      this.state = state
    }
  }

  fun reportIndexingTime(root: VirtualFile, time: Long) {
    synchronized(lock) {
      state.indexTime += time
      state.indexTimeByRoot[root.path] = (state.indexTimeByRoot[root.path] ?: 0) + time
      state.intIncrementModificationCount()
    }
  }

  fun reportFreshIndex() {
    synchronized(lock) {
      state.indexTime = 0
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): VcsLogIndexCollector = project.getService(VcsLogIndexCollector::class.java)
  }
}

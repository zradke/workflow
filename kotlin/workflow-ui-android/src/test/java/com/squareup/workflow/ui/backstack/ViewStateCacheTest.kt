package com.squareup.workflow.ui.backstack

import org.junit.Test

class ViewStateCacheTest {
  @Test fun thing() {
    val cache = ViewStateCache()
    val ableView = View().apply { uniquedKey = Uniqued(Able).key.toString() }

    cache.prepareToUpdate(listOf(Uniqued(Able), Uniqued(Baker)))
        .saveOldView(ableView)
    cache.prepareToUpdate(listOf(Uniqued(Able))).restoreNewView(ableView)
  }
}

object Able
object Baker

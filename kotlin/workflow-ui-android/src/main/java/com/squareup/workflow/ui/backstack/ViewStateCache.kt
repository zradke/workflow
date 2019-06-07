/*
 * Copyright 2018 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.ui.backstack

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.SparseArray
import android.view.View
import android.view.View.BaseSavedState
import com.squareup.workflow.ui.BackStackScreen
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.Uniqued
import com.squareup.workflow.ui.backstack.ViewStateCache.SavedState
import com.squareup.workflow.ui.backstack.ViewStateCache.UpdateTools
import com.squareup.workflow.ui.showRenderingTag

/**
 * Maintains a stack of persisted [view hierarchy states][View.saveHierarchyState].
 *
 * When preparing to show a new screen, call [prepareToUpdate] and use the [UpdateTools]
 * it returns.
 *
 * This class implements [Parcelable] so that it can be preserved from
 * a container view's own [View.saveHierarchyState] method -- call [save] first.
 * A simple container can return [SavedState] from that method rather than
 * creating its own persistence class.
 */
class ViewStateCache private constructor(
  private var viewStates: Map<String, ViewStateFrame>
) : Parcelable {
  constructor() : this(emptyMap())

  /**
   * Provides support to save the state of the current view and possibly restore
   * that of a new view. Returned by [prepareToUpdate].
   */
  interface UpdateTools {
    /** True if saved state was found for this view to restore. */
    val restored: Boolean

    /***
     * Method to run against the outgoing view to save its
     * [view hierarchy state][View.saveHierarchyState].
     */
    fun saveOldView(oldView: View)

    /**
     * Method to run against the incoming view to restore its previously saved
     * [view hierarchy state][View.saveHierarchyState], if there is any.
     * This should be called after the view is inflated / instantiated, and before
     * it is attached to a window.
     */
    fun restoreNewView(newView: View)
  }

  /**
   * To be called when the container is ready to create and show the view for
   * a new [BackStackScreen]. Returns [UpdateTools] to help get the job done.
   */
  fun prepareToUpdate(newStack: List<Uniqued<*>>): UpdateTools {
    require(newStack.isNotEmpty()) { "newStack must not be empty." }

    // Prune any saved states that are no longer in the back stack.
    val deadKeys = viewStates.keys - newStack.map { it.key.toString() }
    viewStates -= deadKeys

    val newScreenKey = newStack.last()
        .key.toString()

    return viewStates[newScreenKey]
        ?.let {
          object : UpdateTools {
            override val restored = true

            override fun saveOldView(oldView: View) {
              // Old view is being popped. Won't be restored, so don't save anything.
            }

            override fun restoreNewView(newView: View) {
              newView.restoreHierarchyState(it.viewState)
              viewStates -= it.key
            }
          }
        }
        ?: object : UpdateTools {
          override val restored = false

          override fun saveOldView(oldView: View) {
            val saved = SparseArray<Parcelable>().apply { oldView.saveHierarchyState(this) }
            val savedKey = oldView.uniquedKey.toString()
            viewStates += savedKey to ViewStateFrame(savedKey, saved)
          }

          override fun restoreNewView(newView: View) {
            // A new view has been pushed, nothing to restore.
          }
        }
  }

  /**
   * To be called from [View.saveHierarchyState] before serializing this instance,
   * to ensure that the state of the currently visible view is saved.
   */
  fun save(currentView: View) {
    val saved = SparseArray<Parcelable>().apply { currentView.saveHierarchyState(this) }
    val newFrame = ViewStateFrame(currentView.uniquedKey.toString(), saved)
    viewStates += newFrame.key to newFrame
  }

  /**
   * Convenience for use in [View.onSaveInstanceState] and [View.onRestoreInstanceState]
   * methods of container views that have no other state or their own to save.
   *
   * More interesting containers should create their own subclass of [BaseSavedState]
   * rather than trying to extend this one.
   */
  class SavedState : BaseSavedState {
    constructor(
      superState: Parcelable?,
      viewStateCache: ViewStateCache
    ) : super(superState) {
      this.viewStateCache = viewStateCache
    }

    constructor(source: Parcel) : super(source) {
      this.viewStateCache = source.readParcelable(SavedState::class.java.classLoader)!!
    }

    val viewStateCache: ViewStateCache

    override fun writeToParcel(
      out: Parcel,
      flags: Int
    ) {
      super.writeToParcel(out, flags)
      out.writeParcelable(viewStateCache, flags)
    }

    companion object CREATOR : Creator<SavedState> {
      override fun createFromParcel(source: Parcel) =
        SavedState(source)

      override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
    }
  }

// region Parcelable

  override fun describeContents(): Int = 0

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int
  ) {
    parcel.writeMap(viewStates)
  }

  companion object CREATOR : Creator<ViewStateCache> {
    override fun createFromParcel(parcel: Parcel): ViewStateCache {
      return mutableMapOf<String, ViewStateFrame>()
          .apply { parcel.readMap(this, ViewStateCache::class.java.classLoader) }
          .let { ViewStateCache(it) }
    }

    override fun newArray(size: Int): Array<ViewStateCache?> = arrayOfNulls(size)
  }

// endregion
}

private val View.uniquedKey: Uniqued.Key<*>
  get() {
    @UseExperimental(ExperimentalWorkflowUi::class)
    return (showRenderingTag!!.showing as Uniqued<*>).key
  }

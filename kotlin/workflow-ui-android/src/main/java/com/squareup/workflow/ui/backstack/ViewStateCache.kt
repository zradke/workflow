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

import com.squareup.workflow.ui.backstack.ViewStateCache.UpdateTools

class View {
  var uniquedKey : String =""
}

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
) {
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
              viewStates -= it.key
            }
          }
        }
        ?: object : UpdateTools {
          override val restored = false

          override fun saveOldView(oldView: View) {
            val savedKey = oldView.uniquedKey.toString()
            viewStates += savedKey to ViewStateFrame(savedKey)
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
    val newFrame = ViewStateFrame(currentView.uniquedKey)
    viewStates += newFrame.key to newFrame
  }
}

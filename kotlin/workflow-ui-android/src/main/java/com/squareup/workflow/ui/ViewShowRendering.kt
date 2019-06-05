/*
 * Copyright 2019 Square Inc.
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
package com.squareup.workflow.ui

import android.view.View

/**
 * Function attached to a view created by [ViewRegistry], to allow it
 * to respond to [View.showRendering].
 */
typealias ViewShowRendering<RenderingT> = (RenderingT) -> Unit

@ExperimentalWorkflowUi
internal data class ShowRenderingTag<RenderingT : Any>(
  val showing: RenderingT,
  val showRendering: ViewShowRendering<RenderingT>
)

@ExperimentalWorkflowUi
fun <RenderingT : Any> View.bindShowRendering(
  initialRendering: RenderingT,
  showRendering: ViewShowRendering<RenderingT>
) {
  setTag(R.id.view_show_rendering_function, ShowRenderingTag(initialRendering, showRendering))
  showRendering.invoke(initialRendering)
}

/**
 * True if this view is able to show [rendering]. Determined by comparing it
 * to what is currently showing. If the current rendering implements [UpdatableRendering],
 * [UpdatableRendering.canUpdateFrom] is called. Otherwise this is true if the old and
 * new renderings are of (exactly) the same type.
 */
@ExperimentalWorkflowUi
fun View.canShowRendering(rendering: Any): Boolean {
  return showRenderingTag?.showing?.matchesRendering(rendering) == true
}

@ExperimentalWorkflowUi
fun <RenderingT : Any> View.showRendering(rendering: RenderingT) {
  showRenderingTag
      ?.let { tag ->
        check(tag.showing.matchesRendering(rendering)) {
          "Expected ${this} to be able to update of ${tag.showing} from $rendering"
        }

        @Suppress("UNCHECKED_CAST")
        bindShowRendering(rendering, tag.showRendering as ViewShowRendering<RenderingT>)
      }
      ?: throw IllegalStateException("showRendering function for $rendering not found on $this.")
}

@ExperimentalWorkflowUi
private val View.showRenderingTag: ShowRenderingTag<*>?
  get() = getTag(R.id.view_show_rendering_function) as? ShowRenderingTag<*>


private fun Any.matchesRendering(other: Any) = renderingsMatch(this, other)

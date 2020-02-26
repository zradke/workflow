package com.squareup.workflow.ui

import android.view.LayoutInflater
import android.view.ViewGroup

typealias ViewBindingInflater<BindingT> = (LayoutInflater, ViewGroup?, Boolean) -> BindingT

/**
 * Creates a [ViewBinding] that [inflates][bindingInflater] a [BindingT] to show renderings of type
 * [RenderingT], using [showRendering].
 *
 * ```
 * val HelloBinding: ViewBinding<Rendering> =
 *   bindViewBinding(HelloGoodbyeLayoutBinding::inflate) { rendering, containerHints ->
 *     helloMessage.text = rendering.message
 *     helloMessage.setOnClickListener { rendering.onClick(Unit) }
 *   }
 * ```
 *
 * If you need to initialize your view before [showRendering] is called, create a [LayoutRunner]
 * and create a binding using `LayoutRunner.bind` instead.
 */
inline fun <BindingT : androidx.viewbinding.ViewBinding, reified RenderingT : Any> bindViewBinding(
  noinline bindingInflater: ViewBindingInflater<BindingT>,
  crossinline showRendering: BindingT.(RenderingT, ContainerHints) -> Unit
): ViewBinding<RenderingT> = LayoutRunner.bind(bindingInflater) { binding ->
  object : LayoutRunner<RenderingT> {
    override fun showRendering(
      rendering: RenderingT,
      containerHints: ContainerHints
    ) = binding.showRendering(rendering, containerHints)
  }
}

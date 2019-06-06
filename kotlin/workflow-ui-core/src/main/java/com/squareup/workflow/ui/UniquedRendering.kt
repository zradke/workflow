package com.squareup.workflow.ui

import kotlin.reflect.KClass

/**
 * Allows renderings that do not implement [UpdatableRendering] themselves
 * to be distinguished by more than just their type.
 */
data class UniquedRendering<W : Any>(
  val wrapped: W,
  val name: String = ""
) : UpdatableRendering<UniquedRendering<W>> {
  data class Key<T : Any>(
    val type: KClass<T>,
    val extension: String = ""
  )

  /**
   * Used as a comparison key by [canUpdateFrom]. Handy for use as a map key.
   */
  val key = Key(wrapped::class, name)

  override fun canUpdateFrom(another: UniquedRendering<W>): Boolean {
    return this.key == another.key && renderingsMatch(this.wrapped, another.wrapped)
  }
}

package com.squareup.workflow.ui

import kotlin.reflect.KClass

/**
 * Allows renderings that do not implement [Compatible] themselves to be distinguished
 * by more than just their type.
 */
data class Uniqued<W : Any>(
  val wrapped: W,
  val name: String = ""
) : Compatible<Uniqued<W>> {
  data class Key<T : Any>(
    val type: KClass<T>,
    val extension: String = ""
  )

  /**
   * Used as a comparison key by [isCompatibleWith]. Handy for use as a map key.
   */
  val key = Key(wrapped::class, name)

  override fun isCompatibleWith(another: Uniqued<W>): Boolean {
    return this.key == another.key && compatible(this.wrapped, another.wrapped)
  }
}

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

/**
 * Implemented by rendering types that need control over the response
 * to [android.view.View.canShowRendering]. Useful when several objects
 * of the same type need to be on screen at the same time.
 */
interface UpdatableRendering<out T : UpdatableRendering<T>> {
  fun canUpdateFrom(another: @UnsafeVariance T): Boolean
}

/**
 * Returns true if [me] and [you] are instances of the same class, unless
 * that class implements [UpdatableRendering]. In that case
 * `me.`[canUpdateFrom][UpdatableRendering.canUpdateFrom]`(you)` must also be true.
 */
fun renderingsMatch(
  me: Any,
  you: Any
): Boolean {
  return when {
    me::class != you::class -> false
    me !is UpdatableRendering<*> -> true
    // If you see a casting error here, it's a lie. https://youtrack.jetbrains.com/issue/KT-31823
    else -> me.canUpdateFrom(you as UpdatableRendering<*>)
  }
}

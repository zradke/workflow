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
package com.squareup.workflow.rx2

import com.squareup.workflow.Workflow
import com.squareup.workflow.stateless
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.Flowable
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.flowables.ConnectableFlowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.TestSubscriber
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlatMapWorkflowTest {

  private class ExpectedException : RuntimeException()

  private val workflow = Workflow.stateless<String, Nothing, String> { input ->
    "rendered: $input"
  }
  private val inputs = PublishSubject.create<String>()
  private val renderings: TestSubscriber<String> =
    inputs.toFlowable(BUFFER)
        .flatMapWorkflow(workflow)
        .map { it.rendering }
        .test()

  @Test fun `doesn't emit until input emitted`() {
    renderings.assertNoValues()
    renderings.assertNotTerminated()
  }

  @Test fun `single input`() {
    inputs.onNext("input")

    renderings.assertValue("rendered: input")
    renderings.assertNotTerminated()
  }

  @Test fun `multiple inputs`() {
    inputs.onNext("one")
    inputs.onNext("two")
    inputs.onNext("three")
    renderings.assertValues("rendered: one", "rendered: two", "rendered: three")
    renderings.assertNotTerminated()
  }

  @Test fun `output doesn't complete after input completes`() {
    inputs.onNext("input")
    inputs.onComplete()
    renderings.assertNotTerminated()
  }

  @Test fun `output errors when input completes before emitting`() {
    inputs.onComplete()
    renderings.assertError { it is NoSuchElementException }
  }

  @Test fun `regular exceptions from subscribers are propagated through replay`() {
    assertErrorFromSubPropagatedThroughRenderings(::ExpectedException) { replay(1) }
  }

  // See https://github.com/square/workflow/issues/399
  @Test fun `fatal exceptions from subscribers are propagated through replay`() {
    // LinkageError is considered a fatal exception by RxJava2.
    assertErrorFromSubPropagatedThroughRenderings(::LinkageError) { replay(1) }
  }

  @Test fun `regular exceptions from subscribers are propagated through publish`() {
    assertErrorFromSubPropagatedThroughRenderings(::ExpectedException) { publish() }
  }

  // See https://github.com/square/workflow/issues/399
  @Test fun `fatal exceptions from subscribers are propagated through publish`() {
    // LinkageError is considered a fatal exception by RxJava2.
    assertErrorFromSubPropagatedThroughRenderings(::LinkageError) { publish() }
  }

  private fun <E : Throwable> assertErrorFromSubPropagatedThroughRenderings(
    exceptionProvider: () -> E,
    shareStrategy: Flowable<Unit>.() -> ConnectableFlowable<Unit>
  ) {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { }
    val updates = Flowable.just(Unit)
        .flatMapWorkflow(workflow)
    val renderings = updates.map { Unit }
        .shareStrategy()
        .autoConnect(1)

    val err = assertFailsWith<OnErrorNotImplementedException> {
      throwUncaughtExceptions {
        renderings.subscribe { throw exceptionProvider() }
      }
    }
    assertTrue(exceptionProvider().javaClass.isInstance(err.cause))
  }

  private fun throwUncaughtExceptions(block: () -> Unit) {
    val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
    var err: Throwable? = null
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
      if (err == null) err = e
      else err!!.addSuppressed(e)
    }
    try {
      block()
    } catch (e: Throwable) {
      err = e.apply { err?.let { addSuppressed(it) } }
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(oldHandler)
      err?.let { throw it }
    }
  }
}

package com.squareup.workflow.ui

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.asWorker
import com.squareup.workflow.onWorkerOutput
import com.squareup.workflow.stateless
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.fail
import org.junit.Test

@UseExperimental(ExperimentalWorkflowUi::class, ExperimentalCoroutinesApi::class)
class WorkflowRunnerViewModelTest {

  private val scope = CoroutineScope(Unconfined)
  @Suppress("RemoveRedundantSpreadOperator")
  private val viewRegistry = ViewRegistry(*emptyArray<ViewBinding<*>>())

  @Test fun snapshotUpdatedOnHostEmission() {
    val snapshot1 = Snapshot.of("one")
    val snapshot2 = Snapshot.of("two")
    val snapshotsChannel = Channel<RenderingAndSnapshot<Unit>>(UNLIMITED)
    val snapshotsFlow = flow { snapshotsChannel.consumeEach { emit(it) } }

    val runner = WorkflowRunnerViewModel(viewRegistry, snapshotsFlow, flowOf(Unit), scope)

    assertThat(runner.getLastSnapshotForTest()).isEqualTo(Snapshot.EMPTY)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot1))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot1)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot2))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot2)
  }

  @Test fun hostCancelledOnCleared() {
    var cancelled = false
    scope.coroutineContext[Job]!!.invokeOnCompletion { e ->
      if (e is CancellationException) cancelled = true
    }

    val runner = WorkflowRunnerViewModel(viewRegistry, emptyFlow(), flowOf(Unit), scope)

    assertThat(cancelled).isFalse()

    runner.clearForTest()
    assertThat(cancelled).isTrue()
  }

  @Test fun crashFromSubscriber() {
    val foo = BehaviorSubject.createDefault("fnord")

    Thread.setDefaultUncaughtExceptionHandler { _, t ->
      throw AssertionError("fml", t)
    }
    foo.subscribe {
      fail("")
    }
  }

  @Test fun errorOnOutputIsNotSwallowed() {
    val outputs = Channel<String>()

    val w = Workflow.stateless<Unit, String, Unit> {
      onWorkerOutput(outputs.asWorker()) { emitOutput(it) }
    }

    val factory = WorkflowRunnerViewModel.Factory(
        w, viewRegistry, flowOf(Unit), null, TestCoroutineDispatcher()
    )
    val model = factory.create(WorkflowRunnerViewModel::class.java)

    model.output.subscribe {
      // This should be failing, but AssertionError is being swallowed by Flow.asFlowable()
      assertThat(it).isEqualTo("Snot")
    }
    runBlocking {
      outputs.send("Fnord")
    }
    TODO("This isn't failing as expected.")
  }

  @Test fun errorOnRenderIsNotSwallowed() {
    TODO()
  }
}

package com.squareup.workflow.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

// If you try to replace isTrue() with isTrue compilation fails.
@Suppress("UsePropertyAccessSyntax")
class UniquedTest {
  object Whut
  object Hey

  @Test fun `same type no value matches`() {
    assertThat(compatible(Uniqued(Hey), Uniqued(Hey))).isTrue()
  }

  @Test fun `same type same value matches`() {
    assertThat(compatible(Uniqued(Hey, "eh"), Uniqued(Hey, "eh"))).isTrue()
  }

  @Test fun `same type diff value matches`() {
    assertThat(compatible(Uniqued(Hey, "blam"), Uniqued(Hey))).isFalse()
  }

  @Test fun `diff type same value no match`() {
    assertThat(compatible(Uniqued(Hey), Uniqued(Whut))).isFalse()
  }

  @Test fun recursion() {
    assertThat(
        compatible(
            Uniqued(Uniqued(Hey, "one")),
            Uniqued(Uniqued(Hey, "one"))
        )
    ).isTrue()

    assertThat(
        compatible(
            Uniqued(Uniqued(Hey, "one")),
            Uniqued(Uniqued(Hey, "two"))
        )
    ).isFalse()

    assertThat(
        compatible(
            Uniqued(Uniqued(Hey)),
            Uniqued(Uniqued(Whut))
        )
    ).isFalse()
  }
}

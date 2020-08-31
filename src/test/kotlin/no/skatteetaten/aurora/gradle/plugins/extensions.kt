package no.skatteetaten.aurora.gradle.plugins

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.SUCCESS

fun BuildResult.taskStatus(taskName: String = ":build", result: TaskOutcome = SUCCESS) =
    assertThat(task(taskName)?.outcome).isEqualTo(result)

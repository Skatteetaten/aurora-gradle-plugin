package no.skatteetaten.aurora.gradle.plugins

import assertk.Assert
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.SUCCESS

fun BuildResult.taskOutcome(taskName: String = ":build") = task(taskName)?.outcome

fun Assert<TaskOutcome?>.isSuccessOrEqualTo(result: TaskOutcome = SUCCESS) = isEqualTo(result)

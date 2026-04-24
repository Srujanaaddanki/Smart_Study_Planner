package com.example.studyplannerai.domain

import com.example.studyplannerai.data.model.PlannedStudyBlock
import com.example.studyplannerai.data.model.Subject
import com.example.studyplannerai.data.model.StudyTask
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class SchedulePlan(
    val blocks: List<PlannedStudyBlock> = emptyList(),
    val suggestedDeadlines: Map<String, Long> = emptyMap()
)

class ScheduleGenerator {

    fun generate(
        tasks: List<StudyTask>,
        subjects: List<Subject>,
        today: LocalDate = LocalDate.now(),
        horizonDays: Long = 7L
    ): SchedulePlan {
        val subjectLookup = subjects.associateBy { it.id }
        val suggestedDeadlines = mutableMapOf<String, Long>()
        val blocks = buildList {
            tasks.filterNot { it.isCompleted }
                .sortedWith(compareBy<StudyTask> { it.deadlineEpochDay }.thenByDescending { it.remainingMinutes })
                .forEach { task ->
                    if (task.remainingMinutes <= 0) return@forEach

                    val originalDeadline = LocalDate.ofEpochDay(task.deadlineEpochDay)
                    val effectiveDeadline = if (originalDeadline.isBefore(today)) {
                        today.plusDays(rescheduleBufferDays(task.remainingMinutes))
                    } else {
                        originalDeadline
                    }

                    if (effectiveDeadline != originalDeadline) {
                        suggestedDeadlines[task.id] = effectiveDeadline.toEpochDay()
                    }

                    val totalDays = ChronoUnit.DAYS.between(today, effectiveDeadline).coerceAtLeast(0) + 1
                    val minutesPerDay = ceil(task.remainingMinutes / totalDays.toDouble()).toInt().coerceAtLeast(15)
                    var remainingMinutes = task.remainingMinutes

                    for (offset in 0 until totalDays) {
                        if (remainingMinutes <= 0 || offset >= horizonDays) break
                        val plannedMinutes = minOf(minutesPerDay, remainingMinutes)
                        val currentDay = today.plusDays(offset)
                        val startMillis = currentDay.atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val endMillis = startMillis + (plannedMinutes * 60000L)
                        val subject = subjectLookup[task.subjectId]

                        add(
                            PlannedStudyBlock(
                                taskId = task.id,
                                taskTitle = task.title,
                                subjectName = subject?.name ?: "Unknown subject",
                                subjectColor = subject?.colorHex ?: "#6200EE",
                                dateEpochDay = currentDay.toEpochDay(),
                                startTime = startMillis,
                                endTime = endMillis,
                                plannedMinutes = plannedMinutes,
                                suggestedDeadlineEpochDay = effectiveDeadline.toEpochDay(),
                                isRescheduled = effectiveDeadline != originalDeadline,
                                status = "Pending"
                            )
                        )
                        remainingMinutes -= plannedMinutes
                    }
                }
        }.sortedBy { it.dateEpochDay }

        return SchedulePlan(
            blocks = blocks,
            suggestedDeadlines = suggestedDeadlines
        )
    }

    private fun rescheduleBufferDays(remainingMinutes: Int): Long {
        return ((remainingMinutes - 1) / 90).toLong().coerceIn(1L, 5L)
    }
}

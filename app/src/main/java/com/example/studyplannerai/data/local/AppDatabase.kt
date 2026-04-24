package com.example.studyplannerai.data.local

import androidx.room.*
import com.example.studyplannerai.data.model.Goal
import com.example.studyplannerai.data.model.StudyPlanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<StudyPlanItem>>

    @Query("SELECT * FROM tasks WHERE day = :date")
    fun getTasksForDate(date: String): Flow<List<StudyPlanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyPlanItem>)

    @Update
    suspend fun updateTask(task: StudyPlanItem)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Delete
    suspend fun deleteTask(task: StudyPlanItem)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}

@Database(entities = [StudyPlanItem::class, Goal::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
}

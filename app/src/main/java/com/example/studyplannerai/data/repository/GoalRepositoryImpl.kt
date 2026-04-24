package com.example.studyplannerai.data.repository

import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.local.GoalDao
import com.example.studyplannerai.data.model.Goal
import com.example.studyplannerai.domain.repository.AuthRepository
import com.example.studyplannerai.domain.repository.GoalRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val goalDao: GoalDao,
    private val authRepository: AuthRepository
) : GoalRepository {

    override fun getGoals(): Flow<List<Goal>> = goalDao.getAllGoals()

    override suspend fun addGoal(goal: Goal): Resource<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
            
            // Save to Room
            goalDao.insertGoal(goal)
            
            // Sync to Firestore
            firestore.collection("users").document(userId).collection("goals")
                .document(goal.id)
                .set(goal)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Success(Unit) // Offline success
        }
    }

    override suspend fun updateGoal(goal: Goal): Resource<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
            goalDao.updateGoal(goal)
            firestore.collection("users").document(userId).collection("goals")
                .document(goal.id)
                .set(goal)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Success(Unit)
        }
    }

    override suspend fun deleteGoal(goal: Goal): Resource<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
            goalDao.deleteGoal(goal)
            firestore.collection("users").document(userId).collection("goals")
                .document(goal.id)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Success(Unit)
        }
    }
}

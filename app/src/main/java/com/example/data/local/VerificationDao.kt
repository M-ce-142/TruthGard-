package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VerificationResult
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationDao {
    @Query("SELECT * FROM verifications ORDER BY timestamp DESC")
    fun getAllVerifications(): Flow<List<VerificationResult>>

    @Query("SELECT * FROM verifications WHERE id = :id")
    suspend fun getVerificationById(id: Long): VerificationResult?

    @Query("SELECT * FROM verifications WHERE claimTitle LIKE '%' || :query || '%' OR easyExplanation LIKE '%' || :query || '%' OR scamType LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchVerifications(query: String): Flow<List<VerificationResult>>

    @Query("SELECT * FROM verifications WHERE verdict = :verdictName ORDER BY timestamp DESC")
    fun getVerificationsByVerdict(verdictName: String): Flow<List<VerificationResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: VerificationResult): Long

    @Query("DELETE FROM verifications WHERE id = :id")
    suspend fun deleteVerification(id: Long)

    @Query("DELETE FROM verifications")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM verifications")
    fun getTotalChecksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM verifications WHERE verdict = 'FAKE' OR verdict = 'POTENTIAL_SCAM'")
    fun getFakeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM verifications WHERE verdict = 'REAL'")
    fun getRealCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM verifications WHERE verdict = 'MISLEADING'")
    fun getMisleadingCount(): Flow<Int>
}

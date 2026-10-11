package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlannedPayoutEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PlannedPayoutDao {

    @Query("SELECT * FROM planned_payouts ORDER BY due_date ASC")
    fun getAllPlannedPayouts(): Flow<List<PlannedPayoutEntity>>

    @Query("SELECT * FROM planned_payouts WHERE UPPER(status) = 'PENDING' ORDER BY due_date ASC")
    fun getPendingPlannedPayouts(): Flow<List<PlannedPayoutEntity>>

    @Query("SELECT * FROM planned_payouts WHERE UPPER(status) = 'PAID' ORDER BY paid_date DESC")
    fun getPaidPlannedPayouts(): Flow<List<PlannedPayoutEntity>>

    @Query("SELECT * FROM planned_payouts WHERE id = :id LIMIT 1")
    fun getPlannedPayoutById(id: Long): Flow<PlannedPayoutEntity?>

    @Query("SELECT * FROM planned_payouts WHERE id = :id LIMIT 1")
    suspend fun getPlannedPayoutByIdImmediate(id: Long): PlannedPayoutEntity?

    @Query("SELECT SUM(amount) FROM planned_payouts WHERE UPPER(status) = 'PENDING'")
    fun getTotalPendingAmount(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payout: PlannedPayoutEntity): Long

    @Update
    suspend fun update(payout: PlannedPayoutEntity)

    @Delete
    suspend fun delete(payout: PlannedPayoutEntity)

    @Query("DELETE FROM planned_payouts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE planned_payouts SET status = :status, paid_date = :paidDate, paid_transaction_id = :txId WHERE id = :id")
    suspend fun updateStatusAndPaidDetails(id: Long, status: String, paidDate: Date?, txId: Long?)
}

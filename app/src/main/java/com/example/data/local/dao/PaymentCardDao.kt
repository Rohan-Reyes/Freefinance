package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PaymentCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentCardDao {

    @Query("SELECT * FROM payment_cards ORDER BY card_type ASC, created_at DESC")
    fun getAllCards(): Flow<List<PaymentCardEntity>>

    @Query("SELECT * FROM payment_cards WHERE id = :id LIMIT 1")
    fun getCardById(id: Long): Flow<PaymentCardEntity?>

    @Query("SELECT * FROM payment_cards WHERE id = :id LIMIT 1")
    suspend fun getCardByIdImmediate(id: Long): PaymentCardEntity?

    @Query("SELECT * FROM payment_cards WHERE linked_account_id = :accountId ORDER BY created_at DESC")
    fun getCardsForAccount(accountId: Long): Flow<List<PaymentCardEntity>>

    @Query("SELECT * FROM payment_cards WHERE card_type = :cardType ORDER BY created_at DESC")
    fun getCardsByType(cardType: String): Flow<List<PaymentCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: PaymentCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<PaymentCardEntity>)

    @Update
    suspend fun updateCard(card: PaymentCardEntity)

    @Delete
    suspend fun deleteCard(card: PaymentCardEntity)

    @Query("DELETE FROM payment_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("UPDATE payment_cards SET current_credit_used = :newUsed WHERE id = :cardId")
    suspend fun updateCreditUsed(cardId: Long, newUsed: Double)

    @Query("UPDATE payment_cards SET is_frozen = :isFrozen WHERE id = :cardId")
    suspend fun updateCardFrozenState(cardId: Long, isFrozen: Boolean)
}

package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * PlannedPayoutEntity: Represents a scheduled one-off disbursement or upcoming planned payment.
 * Distinct from:
 * - Debts (no lender/borrower relationship)
 * - Freelance milestones (no client project deliverable)
 * - Recurring subscriptions (one-time scheduled payout, not auto-renewing cycles)
 *
 * Examples: Annual insurance premium, quarterly tax settlement, scheduled equipment purchase,
 * contractor disbursement, tuition fee, medical procedure payment, holiday travel downpayment.
 */
@Entity(tableName = "planned_payouts")
data class PlannedPayoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "PHP",

    @ColumnInfo(name = "due_date")
    val dueDate: Date,

    @ColumnInfo(name = "target_account_id")
    val targetAccountId: Long? = null,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "payee_or_vendor")
    val payeeOrVendor: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "priority")
    val priority: String = "NORMAL", // HIGH, NORMAL, LOW

    @ColumnInfo(name = "status")
    val status: String = "PENDING", // PENDING, PAID, CANCELLED

    @ColumnInfo(name = "paid_date")
    val paidDate: Date? = null,

    @ColumnInfo(name = "paid_transaction_id")
    val paidTransactionId: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date()
)

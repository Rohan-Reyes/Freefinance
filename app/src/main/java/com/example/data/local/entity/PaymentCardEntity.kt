package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PaymentCardEntity: Represents a saved Debit, Credit, Virtual, or Prepaid Card.
 * - Debit / Virtual / Prepaid cards can be connected directly to an Account or Sub-Account (`linkedAccountId`).
 * - Credit cards track `creditLimit`, `currentCreditUsed`, `statementBalance`, `minimumPaymentDue`,
 *   billing cycle (`statementDay`, `paymentDueDay`), APR, and utilization.
 * - Stores full card details (Card Number, Expiry, CVV, Cardholder Name, Network, Bank, PIN/Notes, Billing Address)
 *   with privacy masking and biometric reveal support.
 */
@Entity(tableName = "payment_cards")
data class PaymentCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "card_name")
    val cardName: String,

    @ColumnInfo(name = "card_type")
    val cardType: String, // "DEBIT", "CREDIT", "VIRTUAL", "PREPAID"

    @ColumnInfo(name = "card_network")
    val cardNetwork: String = "VISA", // "VISA", "MASTERCARD", "AMEX", "JCB", "UNIONPAY", "OTHER"

    @ColumnInfo(name = "issuing_bank")
    val issuingBank: String = "",

    @ColumnInfo(name = "cardholder_name")
    val cardholderName: String = "",

    @ColumnInfo(name = "card_number")
    val cardNumber: String = "",

    @ColumnInfo(name = "expiry_month")
    val expiryMonth: Int = 12,

    @ColumnInfo(name = "expiry_year")
    val expiryYear: Int = 28,

    @ColumnInfo(name = "cvv")
    val cvv: String = "",

    @ColumnInfo(name = "card_pin_hint")
    val cardPinHint: String? = null,

    @ColumnInfo(name = "billing_address")
    val billingAddress: String? = null,

    @ColumnInfo(name = "customer_service_phone")
    val customerServicePhone: String? = null,

    @ColumnInfo(name = "currency_code", defaultValue = "PHP")
    val currencyCode: String = "PHP",

    @ColumnInfo(name = "color", defaultValue = "-14579213")
    val color: Int = -14579213,

    @ColumnInfo(name = "card_finish", defaultValue = "OBSIDIAN_GOLD")
    val cardFinish: String = "OBSIDIAN_GOLD",

    @ColumnInfo(name = "is_virtual", defaultValue = "0")
    val isVirtual: Boolean = false,

    @ColumnInfo(name = "is_frozen", defaultValue = "0")
    val isFrozen: Boolean = false,

    @ColumnInfo(name = "linked_account_id", defaultValue = "NULL")
    val linkedAccountId: Long? = null,

    @ColumnInfo(name = "daily_spend_limit", defaultValue = "0.0")
    val dailySpendLimit: Double = 0.0,

    @ColumnInfo(name = "credit_limit", defaultValue = "0.0")
    val creditLimit: Double = 0.0,

    @ColumnInfo(name = "current_credit_used", defaultValue = "0.0")
    val currentCreditUsed: Double = 0.0,

    @ColumnInfo(name = "statement_balance", defaultValue = "0.0")
    val statementBalance: Double = 0.0,

    @ColumnInfo(name = "minimum_payment_due", defaultValue = "0.0")
    val minimumPaymentDue: Double = 0.0,

    @ColumnInfo(name = "statement_day", defaultValue = "NULL")
    val statementDay: Int? = null,

    @ColumnInfo(name = "payment_due_day", defaultValue = "NULL")
    val paymentDueDay: Int? = null,

    @ColumnInfo(name = "apr_interest_rate", defaultValue = "0.0")
    val aprInterestRate: Float = 0.0f,

    @ColumnInfo(name = "annual_fee", defaultValue = "0.0")
    val annualFee: Double = 0.0,

    @ColumnInfo(name = "rewards_program")
    val rewardsProgram: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
) {
    val isCreditCard: Boolean
        get() = cardType.equals("CREDIT", ignoreCase = true)

    val availableCredit: Double
        get() = (creditLimit - currentCreditUsed).coerceAtLeast(0.0)

    val isOverLimit: Boolean
        get() = isCreditCard && creditLimit > 0.0 && currentCreditUsed > creditLimit

    val utilizationRatio: Float
        get() = if (creditLimit > 0.0) {
            (currentCreditUsed / creditLimit).toFloat().coerceIn(0f, 1f)
        } else 0f

    val utilizationPercent: Int
        get() = if (creditLimit > 0.0) {
            ((currentCreditUsed / creditLimit) * 100.0).toInt()
        } else 0

    val digitsOnlyNumber: String
        get() = cardNumber.filter { it.isDigit() }

    val lastFourDigits: String
        get() {
            val digits = digitsOnlyNumber
            return if (digits.length >= 4) digits.takeLast(4)
            else if (digits.isNotEmpty()) digits.padStart(4, '•')
            else "••••"
        }

    val maskedCardNumber: String
        get() = "••••  ••••  ••••  $lastFourDigits"

    val formattedCardNumber: String
        get() {
            val digits = digitsOnlyNumber
            return if (digits.isNotEmpty()) {
                digits.chunked(4).joinToString("  ")
            } else {
                maskedCardNumber
            }
        }

    val formattedExpiry: String
        get() = "%02d/%02d".format(expiryMonth.coerceIn(1, 12), expiryYear % 100)
}

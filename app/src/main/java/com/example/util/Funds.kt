package com.example.util

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExchangeRateEntity
import com.example.data.local.model.AccountType

/**
 * Splits money into liquid funds and savings. The web app (web/js/logic.js, isSavingsAccount) uses the same rule.
 *
 * Savings (not liquid): accounts whose type is "Savings" or "Locked Deposit / Investment", sub-accounts whose type
 * is a savings kind (Savings, Time Deposit, Emergency Fund, Investment, Vault, Stash, Goals), and any locked account.
 * Liquid: every other account counted in the total (cash, bank, e-wallet, wallets/pockets).
 * Assets are never liquid. Every amount is converted to the base currency.
 */
object Funds {
    private val SAVINGS_SUB_TYPES = setOf(
        "savings", "time deposit", "emergency fund", "investment", "vault", "stash", "goals", "personal goals"
    )

    fun isSavings(a: AccountEntity, now: Long = System.currentTimeMillis()): Boolean =
        a.isLocked ||
            (a.lockedUntil?.time ?: 0L) > now ||
            a.type.equals(AccountType.SAVINGS.displayName, ignoreCase = true) ||
            a.type.equals(AccountType.INVESTMENT.displayName, ignoreCase = true) ||
            (a.subAccountType?.trim()?.lowercase() in SAVINGS_SUB_TYPES)

    data class Split(val liquid: Double, val savings: Double) {
        val total: Double get() = liquid + savings
    }

    /** Liquid and savings totals of the accounts included in the total, in [baseCurrency]. */
    fun split(accounts: List<AccountEntity>, baseCurrency: String, rates: List<ExchangeRateEntity> = emptyList()): Split {
        var liquid = 0.0
        var savings = 0.0
        val now = System.currentTimeMillis()
        for (a in accounts) {
            if (!a.includeInTally) continue
            val v = CurrencyUtils.convert(a.currentBalance, a.currencyCode, baseCurrency, rates)
            if (isSavings(a, now)) savings += v else liquid += v
        }
        return Split(liquid, savings)
    }
}

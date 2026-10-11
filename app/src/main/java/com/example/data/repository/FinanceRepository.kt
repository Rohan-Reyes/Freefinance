package com.example.data.repository

import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.AssetDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.DebtDao
import com.example.data.local.dao.ExchangeRateDao
import com.example.data.local.dao.FreelanceDao
import com.example.data.local.dao.PlannedPayoutDao
import com.example.data.local.dao.ReceiptDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.ExchangeRateEntity
import com.example.data.local.entity.FreelanceProjectEntity
import com.example.data.local.entity.MilestoneEntity
import com.example.data.local.entity.PlannedPayoutEntity
import com.example.data.local.entity.ReceiptEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.model.AccountBalanceSummary
import com.example.data.local.model.BudgetProgress
import com.example.data.local.model.CategoryExpense
import com.example.data.local.model.DebtStatus
import com.example.data.local.model.ProjectWithMilestones
import com.example.data.local.model.RecurrenceRule
import com.example.data.local.model.TransactionStatus
import com.example.data.local.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * FinanceRepository: Single source of truth abstracting Room DAOs for Accounts, Transactions,
 * Categories, Budgets, Debts, Freelance Projects, Quick Pic Receipts, Tangible Assets, and Planned Payouts.
 */
class FinanceRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val debtDao: DebtDao,
    private val freelanceDao: FreelanceDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val receiptDao: ReceiptDao,
    private val assetDao: AssetDao,
    private val plannedPayoutDao: PlannedPayoutDao
) {

    // --- Assets & Valuables ---
    val allAssets: Flow<List<AssetEntity>> = assetDao.getAllAssets()
    val includedAssets: Flow<List<AssetEntity>> = assetDao.getIncludedAssets()
    val totalAssetsValue: Flow<Double?> = assetDao.getTotalAssetsValue()

    suspend fun insertAsset(asset: AssetEntity): Long = assetDao.insertAsset(asset)
    suspend fun updateAsset(asset: AssetEntity) = assetDao.updateAsset(asset)
    suspend fun deleteAsset(asset: AssetEntity) = assetDao.deleteAsset(asset)
    suspend fun deleteAssetById(id: Long) = assetDao.deleteAssetById(id)
    suspend fun getAssetById(id: Long): AssetEntity? = assetDao.getAssetById(id)
    fun getAssetsByCategory(category: String): Flow<List<AssetEntity>> = assetDao.getAssetsByCategory(category)

    // --- Quick Pic Receipts Inbox ---
    val pendingReceipts: Flow<List<ReceiptEntity>> = receiptDao.getPendingReceipts()
    val pendingReceiptCount: Flow<Int> = receiptDao.getPendingReceiptCount()
    val allReceipts: Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()

    suspend fun insertReceipt(receipt: ReceiptEntity): Long = receiptDao.insertReceipt(receipt)

    suspend fun getReceiptByIdImmediate(id: Long): ReceiptEntity? = receiptDao.getReceiptByIdImmediate(id)

    suspend fun updateReceipt(receipt: ReceiptEntity) = receiptDao.updateReceipt(receipt)

    suspend fun deleteReceipt(receipt: ReceiptEntity) = receiptDao.deleteReceipt(receipt)

    suspend fun deleteReceiptById(id: Long) = receiptDao.deleteReceiptById(id)

    suspend fun discardReceipt(id: Long) {
        val r = receiptDao.getReceiptByIdImmediate(id) ?: return
        receiptDao.updateReceipt(r.copy(status = "DISCARDED"))
    }

    /**
     * Convert an inbox receipt into a ledger transaction (Income or Expense).
     */
    suspend fun convertReceiptToTransaction(
        receiptId: Long,
        accountId: Long,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        date: Date,
        note: String?
    ): Long {
        val tx = TransactionEntity(
            accountId = accountId,
            type = type.displayName,
            amount = amount,
            categoryId = categoryId,
            date = date,
            status = TransactionStatus.CLEARED.displayName,
            recurrenceRule = RecurrenceRule.NONE.displayName,
            labels = listOf("Receipt"),
            note = note
        )
        val txId = transactionDao.insertTransaction(tx)

        // Adjust account balance
        applyBalanceChangesForClearedTransaction(tx)

        // Update receipt to CONVERTED
        val receipt = receiptDao.getReceiptByIdImmediate(receiptId)
        if (receipt != null) {
            receiptDao.updateReceipt(
                receipt.copy(
                    status = "CONVERTED",
                    convertedTransactionId = txId,
                    suggestedAmount = amount,
                    suggestedType = type.displayName
                )
            )
        }

        return txId
    }

    // --- Exchange Rate Streams & Operations ---
    val allExchangeRates: Flow<List<ExchangeRateEntity>> = exchangeRateDao.getAllRates()

    suspend fun insertExchangeRate(rate: ExchangeRateEntity): Long = exchangeRateDao.insertRate(rate)

    suspend fun deleteExchangeRate(id: Long) = exchangeRateDao.deleteRate(id)

    // --- Account Streams ---
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    val totalClearedNetWorth: Flow<Double> = accountDao.getTotalClearedNetWorth()

    val accountBalanceSummaries: Flow<List<AccountBalanceSummary>> =
        accountDao.getAccountBalanceSummaries()

    fun getAccountById(id: Long): Flow<AccountEntity?> = accountDao.getAccountById(id)

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) {
        val txSum = accountDao.getClearedTransactionSumForAccount(account.id)
        // Maintain mathematical consistency: current_balance = initial_balance + txSum
        // Therefore calibrated initial_balance = current_balance - txSum
        val calibratedInitial = account.currentBalance - txSum
        accountDao.updateAccount(account.copy(initialBalance = calibratedInitial))
    }

    suspend fun adjustAccountBalance(accountId: Long, targetBalance: Double) {
        val acc = accountDao.getAccountByIdImmediate(accountId) ?: return
        val txSum = accountDao.getClearedTransactionSumForAccount(accountId)
        val calibratedInitial = targetBalance - txSum
        accountDao.updateAccount(acc.copy(
            initialBalance = calibratedInitial,
            currentBalance = targetBalance
        ))
    }

    suspend fun reconcileAccountBalancesSafely() {
        val accounts = accountDao.getAllAccountsImmediate()
        for (acc in accounts) {
            val txSum = accountDao.getClearedTransactionSumForAccount(acc.id)
            if (acc.initialBalance == 0.0 && acc.currentBalance > 0.0) {
                // If account had a positive balance with 0 initial balance, calibrate initialBalance
                val calibratedInitial = acc.currentBalance - txSum
                accountDao.updateAccount(acc.copy(initialBalance = calibratedInitial))
            } else if (acc.currentBalance < 0.0 && acc.initialBalance <= 0.0 && txSum < 0.0) {
                // Keep initialBalance aligned with currentBalance so user can easily adjust it
                val calibratedInitial = acc.currentBalance - txSum
                accountDao.updateAccount(acc.copy(initialBalance = calibratedInitial))
            } else {
                accountDao.recalculateAccountBalance(acc.id)
            }
        }
    }

    suspend fun toggleAccountTally(accountId: Long, include: Boolean) {
        val acc = accountDao.getAccountByIdImmediate(accountId) ?: return
        accountDao.updateAccount(acc.copy(includeInTally = include))
    }

    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    suspend fun insertCategories(categories: List<CategoryEntity>) = categoryDao.insertCategories(categories)

    // --- Transaction Streams ---
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    val categoryExpenses: Flow<List<CategoryExpense>> = transactionDao.getCategoryExpenses()

    val recurringTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecurringTransactions()

    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByAccount(accountId)

    fun getPlannedAndRecurringTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getPlannedAndRecurringTransactions()

    suspend fun recalculateAllAccountBalances() {
        accountDao.recalculateAllAccountBalances()
    }

    suspend fun recalculateAccountBalance(accountId: Long) {
        accountDao.recalculateAccountBalance(accountId)
    }

    suspend fun getTransactionByIdImmediate(id: Long): TransactionEntity? =
        transactionDao.getTransactionByIdImmediate(id)

    suspend fun stopRecurrence(transactionId: Long) = transactionDao.stopRecurrence(transactionId)

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        if (transaction.status.equals("Cleared", ignoreCase = true)) {
            accountDao.recalculateAccountBalance(transaction.accountId)
            transaction.toAccountId?.let { accountDao.recalculateAccountBalance(it) }
        }
    }

    suspend fun deleteTransactionById(id: Long) {
        val tx = transactionDao.getTransactionByIdImmediate(id)
        transactionDao.deleteTransactionById(id)
        if (tx != null && tx.status.equals("Cleared", ignoreCase = true)) {
            accountDao.recalculateAccountBalance(tx.accountId)
            tx.toAccountId?.let { accountDao.recalculateAccountBalance(it) }
        }
    }

    suspend fun deleteOrphanedPlannedTransactions() = transactionDao.deleteOrphanedPlannedTransactions()

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
        if (transaction.status.equals("Cleared", ignoreCase = true)) {
            accountDao.recalculateAccountBalance(transaction.accountId)
            transaction.toAccountId?.let { accountDao.recalculateAccountBalance(it) }
        }
    }

    suspend fun updateClearedTransaction(newTx: TransactionEntity, oldTx: TransactionEntity? = null) {
        transactionDao.updateTransaction(newTx)
        accountDao.recalculateAccountBalance(newTx.accountId)
        newTx.toAccountId?.let { accountDao.recalculateAccountBalance(it) }
        if (oldTx != null) {
            if (oldTx.accountId != newTx.accountId) {
                accountDao.recalculateAccountBalance(oldTx.accountId)
            }
            if (oldTx.toAccountId != null && oldTx.toAccountId != newTx.toAccountId) {
                accountDao.recalculateAccountBalance(oldTx.toAccountId)
            }
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val txId = transactionDao.insertTransaction(transaction)
        if (transaction.status.equals("Cleared", ignoreCase = true)) {
            accountDao.recalculateAccountBalance(transaction.accountId)
            transaction.toAccountId?.let { accountDao.recalculateAccountBalance(it) }
        }
        return txId
    }

    suspend fun addRecurringTransaction(
        amount: Double,
        type: TransactionType,
        accountId: Long,
        categoryId: Long? = null,
        recurrenceRule: RecurrenceRule,
        startDate: Date = Date(),
        note: String? = null
    ): Long {
        val transaction = TransactionEntity(
            amount = amount,
            type = type.displayName,
            date = startDate,
            status = TransactionStatus.PLANNED.displayName,
            accountId = accountId,
            categoryId = categoryId,
            recurrenceRule = recurrenceRule.displayName,
            note = note
        )
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun addQuickTransaction(
        amount: Double,
        type: TransactionType,
        accountId: Long,
        toAccountId: Long? = null,
        categoryId: Long? = null,
        labels: List<String> = emptyList(),
        note: String? = null,
        proofImageUri: String? = null
    ): Long {
        val transaction = TransactionEntity(
            amount = amount,
            type = type.displayName,
            date = Date(),
            status = TransactionStatus.CLEARED.displayName,
            accountId = accountId,
            toAccountId = toAccountId,
            categoryId = categoryId,
            labels = labels,
            note = note,
            recurrenceRule = RecurrenceRule.NONE.displayName,
            proofImageUri = proofImageUri
        )

        val txId = transactionDao.insertTransaction(transaction)
        accountDao.recalculateAccountBalance(accountId)
        toAccountId?.let { accountDao.recalculateAccountBalance(it) }
        return txId
    }

    private suspend fun applyBalanceChangesForClearedTransaction(transaction: TransactionEntity) {
        val sourceAccount = accountDao.getAccountByIdImmediate(transaction.accountId) ?: return

        when (transaction.type) {
            TransactionType.INCOME.displayName -> {
                accountDao.updateCurrentBalance(
                    sourceAccount.id,
                    sourceAccount.currentBalance + transaction.amount
                )
            }
            TransactionType.EXPENSE.displayName -> {
                accountDao.updateCurrentBalance(
                    sourceAccount.id,
                    sourceAccount.currentBalance - transaction.amount
                )
            }
            TransactionType.TRANSFER.displayName -> {
                accountDao.updateCurrentBalance(
                    sourceAccount.id,
                    sourceAccount.currentBalance - transaction.amount
                )
                transaction.toAccountId?.let { destinationId ->
                    val destAccount = accountDao.getAccountByIdImmediate(destinationId)
                    if (destAccount != null) {
                        accountDao.updateCurrentBalance(
                            destAccount.id,
                            destAccount.currentBalance + transaction.amount
                        )
                    }
                }
            }
        }
    }

    // --- Category Streams ---
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    val rootCategories: Flow<List<CategoryEntity>> = categoryDao.getRootCategories()

    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>> = categoryDao.getSubcategories(parentId)

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // --- Budget Streams ---
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    val budgetProgressList: Flow<List<BudgetProgress>> = budgetDao.getBudgetProgressByCategory()

    fun getBudgetProgressById(budgetId: Long): Flow<BudgetProgress?> =
        budgetDao.getBudgetProgressById(budgetId)

    suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    // --- Debt Streams & Operations ---
    val allDebts: Flow<List<DebtEntity>> = debtDao.getAllDebts()

    val lentDebts: Flow<List<DebtEntity>> = debtDao.getDebtsByTypeAndStatus("Lent", "Active")

    val borrowedDebts: Flow<List<DebtEntity>> = debtDao.getDebtsByTypeAndStatus("Borrowed", "Active")

    val topReceivables: Flow<List<DebtEntity>> = debtDao.getTopReceivables(3)

    val totalActiveLent: Flow<Double> = debtDao.getTotalActiveLent()

    val totalActiveBorrowed: Flow<Double> = debtDao.getTotalActiveBorrowed()

    fun getDebtsByType(type: String): Flow<List<DebtEntity>> = debtDao.getDebtsByType(type)

    suspend fun getDebtByIdImmediate(id: Long): DebtEntity? = debtDao.getDebtByIdImmediate(id)

    suspend fun insertDebt(debt: DebtEntity): Long = debtDao.insertDebt(debt)

    suspend fun updateDebt(debt: DebtEntity) = debtDao.updateDebt(debt)

    suspend fun deleteDebt(debt: DebtEntity) = debtDao.deleteDebt(debt)

    suspend fun settleDebt(debtId: Long, accountId: Long? = null) {
        val debt = debtDao.getDebtByIdImmediate(debtId) ?: return
        val amountToSettle = debt.remainingAmount
        debtDao.updateRemainingAndStatus(debtId, 0.0, DebtStatus.SETTLED.displayName)

        if (accountId != null && amountToSettle > 0.0001) {
            val isLent = debt.type.equals("Lent", ignoreCase = true)
            val txType = if (isLent) TransactionType.INCOME else TransactionType.EXPENSE
            val note = if (isLent) {
                "Received Lent Repayment from ${debt.personName}"
            } else {
                "Repaid Borrowed Debt to ${debt.personName}"
            }
            addQuickTransaction(
                amount = amountToSettle,
                type = txType,
                accountId = accountId,
                labels = listOf("Debt Repayment", if (isLent) "Lent" else "Borrowed", debt.personName),
                note = note
            )
        }
    }

    suspend fun payDebtPortion(debtId: Long, paymentAmount: Double, accountId: Long? = null) {
        val debt = debtDao.getDebtByIdImmediate(debtId) ?: return
        val actualPayment = paymentAmount.coerceAtMost(debt.remainingAmount).coerceAtLeast(0.0)
        val newRemaining = (debt.remainingAmount - actualPayment).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.0001) DebtStatus.SETTLED.displayName else DebtStatus.ACTIVE.displayName
        debtDao.updateRemainingAndStatus(debtId, newRemaining, newStatus)

        if (accountId != null && actualPayment > 0.0001) {
            val isLent = debt.type.equals("Lent", ignoreCase = true)
            val txType = if (isLent) TransactionType.INCOME else TransactionType.EXPENSE
            val note = if (isLent) {
                "Received Partial Lent Repayment from ${debt.personName}"
            } else {
                "Partial Repayment for Borrowed Debt to ${debt.personName}"
            }
            addQuickTransaction(
                amount = actualPayment,
                type = txType,
                accountId = accountId,
                labels = listOf("Debt Repayment", if (isLent) "Lent" else "Borrowed", debt.personName),
                note = note
            )
        }
    }

    // --- Freelance Commission Streams & Operations ---
    val activeProjectsWithMilestones: Flow<List<ProjectWithMilestones>> =
        freelanceDao.getActiveProjectsWithMilestones()

    val allProjectsWithMilestones: Flow<List<ProjectWithMilestones>> =
        freelanceDao.getAllProjectsWithMilestones()

    val pendingMilestones: Flow<List<MilestoneEntity>> = freelanceDao.getPendingMilestones()

    suspend fun insertProject(project: FreelanceProjectEntity): Long =
        freelanceDao.insertProject(project)

    suspend fun updateProject(project: FreelanceProjectEntity) =
        freelanceDao.updateProject(project)

    suspend fun deleteProject(project: FreelanceProjectEntity) =
        freelanceDao.deleteProject(project)

    suspend fun insertMilestone(milestone: MilestoneEntity): Long =
        freelanceDao.insertMilestone(milestone)

    suspend fun updateMilestone(milestone: MilestoneEntity) =
        freelanceDao.updateMilestone(milestone)

    suspend fun deleteMilestone(milestone: MilestoneEntity) =
        freelanceDao.deleteMilestone(milestone)

    /**
     * Mark Milestone as Paid:
     * 1. Logs an Income transaction in the ledger for the chosen account.
     * 2. Credits the target account's current balance.
     * 3. Updates milestone status to "Paid".
     */
    suspend fun markMilestoneAsPaid(milestoneId: Long, accountId: Long) {
        val milestone = freelanceDao.getMilestoneById(milestoneId) ?: return
        if (milestone.status.equals("Paid", ignoreCase = true)) return

        // 1. Log Income transaction
        addQuickTransaction(
            amount = milestone.amount,
            type = TransactionType.INCOME,
            accountId = accountId,
            labels = listOf("Freelance", "Milestone"),
            note = "Freelance Milestone: ${milestone.title}"
        )

        // 2. Mark milestone as paid
        freelanceDao.updateMilestoneStatus(milestoneId, "Paid", Date())
    }

    // --- Planned Payouts (Scheduled One-Off Commitments) ---
    val allPlannedPayouts: Flow<List<PlannedPayoutEntity>> = plannedPayoutDao.getAllPlannedPayouts()
    val pendingPlannedPayouts: Flow<List<PlannedPayoutEntity>> = plannedPayoutDao.getPendingPlannedPayouts()
    val paidPlannedPayouts: Flow<List<PlannedPayoutEntity>> = plannedPayoutDao.getPaidPlannedPayouts()
    val totalPendingPayoutsAmount: Flow<Double?> = plannedPayoutDao.getTotalPendingAmount()

    suspend fun insertPlannedPayout(payout: PlannedPayoutEntity): Long = plannedPayoutDao.insert(payout)
    suspend fun updatePlannedPayout(payout: PlannedPayoutEntity) = plannedPayoutDao.update(payout)
    suspend fun deletePlannedPayout(payout: PlannedPayoutEntity) = plannedPayoutDao.delete(payout)
    suspend fun deletePlannedPayoutById(id: Long) = plannedPayoutDao.deleteById(id)
    suspend fun getPlannedPayoutByIdImmediate(id: Long): PlannedPayoutEntity? = plannedPayoutDao.getPlannedPayoutByIdImmediate(id)

    /**
     * Mark Planned Payout as Paid:
     * 1. Logs an Expense transaction for the designated account.
     * 2. Deducts account balance.
     * 3. Sets payout status to PAID, records paid date and transaction ID.
     * Returns the created TransactionEntity.
     */
    suspend fun markPlannedPayoutAsPaid(
        payoutId: Long,
        accountId: Long,
        note: String? = null,
        actualAmount: Double? = null
    ): Pair<PlannedPayoutEntity, TransactionEntity>? {
        val payout = plannedPayoutDao.getPlannedPayoutByIdImmediate(payoutId) ?: return null
        val payAmount = actualAmount ?: payout.amount
        val txNote = note ?: ("Planned Payout: " + payout.title + (if (!payout.payeeOrVendor.isNullOrBlank()) " (${payout.payeeOrVendor})" else ""))

        val tx = TransactionEntity(
            amount = payAmount,
            type = TransactionType.EXPENSE.displayName,
            date = Date(),
            status = TransactionStatus.CLEARED.displayName,
            accountId = accountId,
            categoryId = payout.categoryId,
            labels = listOf("Planned Payout", payout.title),
            note = txNote,
            recurrenceRule = RecurrenceRule.NONE.displayName
        )
        val txId = insertTransaction(tx)
        val insertedTx = tx.copy(id = txId)

        val updatedPayout = payout.copy(
            status = "PAID",
            paidDate = Date(),
            paidTransactionId = txId
        )
        plannedPayoutDao.update(updatedPayout)
        return Pair(updatedPayout, insertedTx)
    }

    suspend fun unmarkPlannedPayoutPaid(payout: PlannedPayoutEntity, transactionId: Long?) {
        if (transactionId != null) {
            deleteTransactionById(transactionId)
        }
        val reverted = payout.copy(
            status = "PENDING",
            paidDate = null,
            paidTransactionId = null
        )
        plannedPayoutDao.update(reverted)
    }
}

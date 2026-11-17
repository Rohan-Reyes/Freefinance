package com.example.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickBalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val pendingAppIntent = FinanceAppWidgetManager.getAppOpenPendingIntent(context)
            val pendingQuickAddIntent = FinanceAppWidgetManager.getAppOpenPendingIntent(context, quickAdd = true)

            CoroutineScope(Dispatchers.IO).launch {
                com.example.util.LiveRates.init(context)
                val db = AppDatabase.getDatabase(context)
                val accounts = try { db.accountDao().getAllAccounts().first() } catch (e: Exception) { emptyList() }
                val tallyAccounts = accounts.filter { it.includeInTally }
                val rates = try { db.exchangeRateDao().getAllRates().first() } catch (e: Exception) { emptyList() }
                val baseCurrency = try { UserPreferencesRepository(context).baseCurrency.first() } catch (e: Exception) { "PHP" }
                // Liquid funds only: savings / locked accounts and assets are not liquid.
                val funds = com.example.util.Funds.split(accounts, baseCurrency, rates)
                val totalBalance = funds.liquid
                val savingsInfo = if (funds.savings != 0.0) "Savings ${CurrencyUtils.formatCurrency(funds.savings, baseCurrency)}" else "All accounts"
                val count = tallyAccounts.size

                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_quick_balance).apply {
                            setTextViewText(R.id.tv_total_balance, CurrencyUtils.formatCurrency(totalBalance, baseCurrency))
                            setTextViewText(R.id.tv_accounts_count, "$count ${if (count == 1) "Account" else "Accounts"}")
                            setTextViewText(R.id.tv_sub_info, savingsInfo)

                            setOnClickPendingIntent(R.id.widget_root, pendingAppIntent)
                            setOnClickPendingIntent(R.id.btn_quick_add, pendingQuickAddIntent)
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }
}

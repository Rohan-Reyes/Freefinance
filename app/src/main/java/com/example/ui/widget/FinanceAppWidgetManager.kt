package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.model.BudgetPeriod
import com.example.data.local.model.TransactionType
import com.example.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FinanceAppWidgetManager {

    fun isPinSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
            appWidgetManager?.isRequestPinAppWidgetSupported == true
        } else {
            false
        }
    }

    fun requestPinWidget(context: Context, providerClass: Class<out AppWidgetProvider>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val myProvider = ComponentName(context, providerClass)
                val successIntent = Intent(context, providerClass).apply {
                    action = "com.example.ACTION_WIDGET_PINNED"
                }
                val successPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    successIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                return appWidgetManager.requestPinAppWidget(myProvider, null, successPendingIntent)
            }
        }
        return false
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        fun updateProvider(providerClass: Class<out AppWidgetProvider>, updateAction: (IntArray) -> Unit) {
            val component = ComponentName(context, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateAction(ids)
                try {
                    val updateIntent = Intent(context, providerClass).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(updateIntent)
                } catch (_: Exception) {
                }
            }
        }

        updateProvider(QuickBalanceWidgetProvider::class.java) { ids ->
            QuickBalanceWidgetProvider.updateWidgets(context, appWidgetManager, ids)
        }

        updateProvider(QuickActionsWidgetProvider::class.java) { ids ->
            QuickActionsWidgetProvider.updateWidgets(context, appWidgetManager, ids)
        }

        updateProvider(BudgetBurnWidgetProvider::class.java) { ids ->
            BudgetBurnWidgetProvider.updateWidgets(context, appWidgetManager, ids)
        }

        updateProvider(MonthlyTrendsWidgetProvider::class.java) { ids ->
            MonthlyTrendsWidgetProvider.updateWidgets(context, appWidgetManager, ids)
        }

        updateProvider(BalanceSquareWidgetProvider::class.java) { ids ->
            BalanceSquareWidgetProvider.updateWidgets(context, appWidgetManager, ids)
        }

        updateProvider(BudgetSquareWidgetProvider::class.java) { ids ->
            BudgetSquareWidgetProvider.updateWidgets(context, appWidgetManager, ids)
        }

        updateProvider(QuickActionsSquareWidgetProvider::class.java) { ids ->
            QuickActionsSquareWidgetProvider.updateWidgets(context, appWidgetManager, ids)
        }
    }

    fun getAppOpenPendingIntent(
        context: Context,
        destination: String? = null,
        quickAdd: Boolean = false,
        quickPic: Boolean = false
    ): PendingIntent {
        val dest = destination?.uppercase()?.trim()
        val actionName = "com.example.action.OPEN_${dest ?: "DASHBOARD"}_QA_${quickAdd}_QP_${quickPic}"
        val intent = Intent(context, MainActivity::class.java).apply {
            action = actionName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (dest != null) {
                putExtra("OPEN_DESTINATION", dest)
            }
            putExtra("QUICK_ADD", quickAdd)
            putExtra("QUICK_PIC", quickPic)
        }
        val requestCode = (actionName.hashCode()).let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

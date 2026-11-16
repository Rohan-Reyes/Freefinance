package com.example

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.PlannedPayoutEntity
import com.example.data.local.entity.ReceiptEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.AppThemePreset
import com.example.data.preferences.HeroBackgroundStyle
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.sync.DataSyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRoundTripTest {
  private val ctx = ApplicationProvider.getApplicationContext<Context>()
  private fun db() = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
  private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

  @Test
  fun `android export contains everything and imports back`() = runBlocking {
    val src = db()
    val photo = File(ctx.cacheDir, "proof.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6)) }
    val receiptPhoto = File(ctx.cacheDir, "receipt.jpg").apply { writeBytes(byteArrayOf(9, 8, 7)) }
    src.categoryDao().insertCategory(CategoryEntity(id = 1, name = "Food", iconResId = "ic_food", type = "Expense", color = 1))
    src.accountDao().insertAccount(AccountEntity(id = 1, name = "Wallet", type = "Cash", initialBalance = 100.0, currentBalance = 90.0, color = 2))
    src.transactionDao().insertTransaction(TransactionEntity(id = 1, accountId = 1, type = "Expense", amount = 10.0, categoryId = 1, date = Date(), status = "Cleared", recurrenceRule = "None", labels = listOf("a"), proofImageUri = photo.absolutePath))
    src.assetDao().insertAsset(AssetEntity(id = 1, name = "Car", category = "Vehicle", estimatedValue = 5000.0, purchaseDate = Date(1_700_000_000_000)))
    src.plannedPayoutDao().insert(PlannedPayoutEntity(id = 1, title = "Tax", amount = 50.0, dueDate = Date(), targetAccountId = 1))
    src.receiptDao().insertReceipt(ReceiptEntity(id = 1, imagePath = receiptPhoto.absolutePath, note = "lunch"))
    val prefs = UserPreferencesRepository(ctx)
    prefs.setAppThemePreset(AppThemePreset.SUNSET_GLOW); prefs.setAppThemeMode(AppThemeMode.DARK); prefs.setHideMoney(true)

    val json = DataSyncManager(src, ctx).exportStateToJson("PHP")
    val root = JSONObject(json)
    assertEquals(2, root.getInt("version"))
    assertEquals(1, root.getJSONArray("assets").length())
    assertEquals(1, root.getJSONArray("plannedPayouts").length())
    assertEquals(1, root.getJSONArray("receiptsInbox").length())
    assertEquals("SUNSET_GLOW", root.getJSONObject("settings").getString("app_theme_preset"))
    assertTrue(root.getJSONObject("settings").getBoolean("hide_money"))
    assertEquals(2, root.getJSONObject("images").length())
    assertEquals(sha(photo.readBytes()), root.getJSONArray("transactions").getJSONObject(0).getString("proofSha256"))

    // Restore into a fresh database with the photo files gone and settings changed.
    photo.delete(); receiptPhoto.delete()
    prefs.setAppThemePreset(AppThemePreset.OCEAN_INDIGO); prefs.setAppThemeMode(AppThemeMode.LIGHT); prefs.setHideMoney(false)
    val dst = db()
    val n = DataSyncManager(dst, ctx).importStateFromJson(json).getOrThrow()
    assertEquals(6, n)
    assertEquals("Car", dst.assetDao().getAllAssets().first().single().name)
    assertEquals(1L, dst.plannedPayoutDao().getAllPlannedPayouts().first().single().targetAccountId)
    val tx = dst.transactionDao().getAllTransactions().first().single()
    assertTrue(File(tx.proofImageUri!!).readBytes().contentEquals(byteArrayOf(1, 2, 3, 4, 5, 6)))
    val rc = dst.receiptDao().getAllReceipts().first().single()
    assertTrue(File(rc.imagePath).readBytes().contentEquals(byteArrayOf(9, 8, 7)))
    assertEquals(AppThemePreset.SUNSET_GLOW, prefs.appThemePreset.first())
    assertEquals(AppThemeMode.DARK, prefs.appThemeMode.first())
    assertEquals(true, prefs.hideMoney.first())
    assertEquals(90.0, dst.accountDao().getAllAccounts().first().single().currentBalance, 0.0) // phone backups keep stored balances
  }

  @Test
  fun `web export imports with ids photos and settings`() = runBlocking {
    val span = 1L shl 40
    val img = byteArrayOf(5, 5, 5)
    val h = sha(img)
    val web = """
      {"version":2,"exportedAt":1,"baseCurrency":"USD",
       "settings":{"base_currency":"USD","app_theme_preset":"GOLDEN_LUXE","app_theme_mode":"LIGHT","hero_background_style":"GOLDEN_LUXE"},
       "accounts":[{"id":${3 * span + 1},"name":"Bank","type":"Bank Account","initial_balance":200,"current_balance":0,"color":-1,"interest_rate_pa":0,"compounding_frequency":"NONE","currency_code":"USD","is_locked":0,"include_in_tally":1}],
       "categories":[{"id":${3 * span + 2},"name":"Fun","icon_res_id":"ic_fun","type":"Expense","color":-2,"parent_category_id":null}],
       "transactions":[{"id":${3 * span + 3},"account_id":${3 * span + 1},"type":"Expense","amount":25,"category_id":${3 * span + 2},"date":1700000000000,"status":"Cleared","recurrence_rule":"None","labels":"[\"x\",\"y\"]","note":"n","proof_image_uri":null,"proof_sha256":"$h"}],
       "freelance_projects":[],"exchange_rates":[],"payment_cards":[],"budgets":[],"debts":[],"milestones":[],
       "assets":[{"id":${3 * span + 4},"name":"Gold","category":"Precious Metals","estimated_value":10,"purchase_price":0,"purchase_date":null,"currency_code":"USD","notes":null,"icon_name":"ic_asset","color":-1,"include_in_net_worth":0,"created_at":5}],
       "planned_payouts":[],"receipts_inbox":[],
       "images":{"$h":"${Base64.encodeToString(img, Base64.NO_WRAP)}"}}
    """.trimIndent()
    val dst = db()
    val n = DataSyncManager(dst, ctx).importStateFromJson(web).getOrThrow()
    assertEquals(4, n)
    val acc = dst.accountDao().getAllAccounts().first().single()
    assertEquals(-(3 * span + 1), acc.id) // another device's row -> negative local id, like server sync
    assertEquals(175.0, acc.currentBalance, 0.001) // recomputed: 200 - 25
    val tx = dst.transactionDao().getAllTransactions().first().single()
    assertEquals(acc.id, tx.accountId)
    assertEquals(listOf("x", "y"), tx.labels)
    assertTrue(File(tx.proofImageUri!!).readBytes().contentEquals(img))
    assertEquals(false, dst.assetDao().getAllAssets().first().single().includeInNetWorth)
    val prefs = UserPreferencesRepository(ctx)
    assertEquals(AppThemePreset.GOLDEN_LUXE, prefs.appThemePreset.first())
    assertEquals(HeroBackgroundStyle.GOLDEN_LUXE, prefs.heroBackgroundStyle.first())
    assertEquals("USD", prefs.baseCurrency.first())
  }
}

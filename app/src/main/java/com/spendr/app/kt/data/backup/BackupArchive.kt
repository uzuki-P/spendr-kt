package com.spendr.app.kt.data.backup

import androidx.room.withTransaction
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.QuickAddEntity
import com.spendr.app.kt.data.db.entity.TransactionEntity
import com.spendr.app.kt.data.db.entity.ReceiptItemEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

const val ARCHIVE_VERSION = 1
const val BACKUP_ENTRY_NAME = "spendr_backup.json"

/**
 * Backup archive: a single-entry STORED zip holding the whole backup as JSON
 * (camelCase fields). Compatible with the RN app's hand-rolled archives, which
 * are also stored-method zips with zeroed timestamps.
 */
object BackupArchive {

    // --- encode ---

    fun encode(manifest: JSONObject, categories: JSONArray, transactions: JSONArray, quickAdd: JSONArray, receiptItems: JSONArray = JSONArray()): ByteArray {
        val backup = JSONObject().apply {
            put("manifest", manifest)
            put("categories", categories)
            put("transactions", transactions)
            put("quickAdd", quickAdd)
            put("receiptItems", receiptItems)
        }
        val body = backup.toString().toByteArray(Charsets.UTF_8)
        val checksum = CRC32().apply { update(body) }

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            val entry = ZipEntry(BACKUP_ENTRY_NAME).apply {
                method = ZipEntry.STORED
                size = body.size.toLong()
                compressedSize = body.size.toLong()
                crc = checksum.value
                time = 0
            }
            zip.putNextEntry(entry)
            zip.write(body)
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    // --- decode ---

    fun decode(bytes: ByteArray): JSONObject {
        val body = extractBody(bytes)
        return JSONObject(String(body, Charsets.UTF_8))
    }

    private fun extractBody(bytes: ByteArray): ByteArray {
        val stream = ZipInputStream(ByteArrayInputStream(bytes))
        stream.use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == BACKUP_ENTRY_NAME) {
                    return zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        throw BackupException("This is not a supported Spendr backup archive.")
    }

    // --- JSON schema ---

    fun archiveFileName(createdAtMs: Long = System.currentTimeMillis()): String {
        val zoned = Instant.ofEpochMilli(createdAtMs).atZone(ZoneId.systemDefault())
        val stamp = DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss").format(zoned)
        return "spendr_backup_$stamp.zip"
    }

    fun buildManifest(categories: Int, transactions: Int, quickAdd: Int, createdAt: Long = System.currentTimeMillis()): JSONObject =
        JSONObject().apply {
            put("format", "spendr_backup")
            put("version", ARCHIVE_VERSION)
            put("createdAt", createdAt)
            put(
                "counts",
                JSONObject().apply {
                    put("categories", categories)
                    put("transactions", transactions)
                    put("quickAdd", quickAdd)
                },
            )
        }

    fun validate(backup: JSONObject) {
        val manifest = backup.optJSONObject("manifest")
        val valid = manifest != null &&
            manifest.optString("format") == "spendr_backup" &&
            manifest.optInt("version") == ARCHIVE_VERSION &&
            backup.optJSONArray("categories") != null &&
            backup.optJSONArray("transactions") != null &&
            backup.optJSONArray("quickAdd") != null
        if (!valid) {
            throw BackupException("This backup is incompatible with this version of Spendr.")
        }
    }

    fun categoryToJson(c: CategoryEntity): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("icon", c.icon)
        put("color", c.color)
        put("sortOrder", c.sortOrder)
        put("isDefault", c.isDefault)
        put("createdAt", c.createdAt)
        put("updatedAt", c.updatedAt)
    }

    fun transactionToJson(t: TransactionEntity): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("paidAmount", t.paidAmount)
        t.originalAmount?.let { put("originalAmount", it) } ?: put("originalAmount", JSONObject.NULL)
        t.discountAmount?.let { put("discountAmount", it) } ?: put("discountAmount", JSONObject.NULL)
        t.discountType?.let { put("discountType", it) } ?: put("discountType", JSONObject.NULL)
        put("categoryId", t.categoryId)
        t.note?.let { put("note", it) } ?: put("note", JSONObject.NULL)
        t.merchant?.let { put("merchant", it) } ?: put("merchant", JSONObject.NULL)
        t.tags?.let { put("tags", it) } ?: put("tags", JSONObject.NULL)
        put("date", t.date)
        put("createdAt", t.createdAt)
        put("updatedAt", t.updatedAt)
        put("type", t.type)
    }

    fun receiptItemToJson(item: ReceiptItemEntity): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("transactionId", item.transactionId)
        put("name", item.name)
        put("paidAmount", item.paidAmount)
        put("quantity", item.quantity)
        put("sortOrder", item.sortOrder)
    }

    fun receiptItemFromJson(o: JSONObject): ReceiptItemEntity = ReceiptItemEntity(
        id = o.getLong("id"),
        transactionId = o.getLong("transactionId"),
        name = o.getString("name"),
        paidAmount = o.getLong("paidAmount"),
        quantity = o.optString("quantity", "1"),
        sortOrder = o.optInt("sortOrder", 0),
    )

    fun quickAddToJson(q: QuickAddEntity): JSONObject = JSONObject().apply {
        put("id", q.id)
        put("label", q.label)
        put("categoryId", q.categoryId)
        q.note?.let { put("note", it) } ?: put("note", JSONObject.NULL)
        q.paidAmount?.let { put("paidAmount", it) } ?: put("paidAmount", JSONObject.NULL)
        q.merchant?.let { put("merchant", it) } ?: put("merchant", JSONObject.NULL)
        q.tags?.let { put("tags", it) } ?: put("tags", JSONObject.NULL)
        put("sortOrder", q.sortOrder)
    }

    fun categoryFromJson(o: JSONObject): CategoryEntity = CategoryEntity(
        id = o.getLong("id"),
        name = o.getString("name"),
        icon = o.getString("icon"),
        color = o.getString("color"),
        sortOrder = o.getDouble("sortOrder"),
        isDefault = o.getBoolean("isDefault"),
        createdAt = o.getLong("createdAt"),
        updatedAt = o.getLong("updatedAt"),
    )

    fun transactionFromJson(o: JSONObject): TransactionEntity = TransactionEntity(
        id = o.getLong("id"),
        paidAmount = o.getLong("paidAmount"),
        originalAmount = o.optLongOrNull("originalAmount"),
        discountAmount = o.optLongOrNull("discountAmount"),
        discountType = o.optStringOrNull("discountType"),
        categoryId = o.getLong("categoryId"),
        note = o.optStringOrNull("note"),
        merchant = o.optStringOrNull("merchant"),
        tags = o.optStringOrNull("tags"),
        date = o.getLong("date"),
        createdAt = o.getLong("createdAt"),
        updatedAt = o.getLong("updatedAt"),
        type = o.optString("type", "standard"),
    )

    fun quickAddFromJson(o: JSONObject): QuickAddEntity = QuickAddEntity(
        id = o.getLong("id"),
        label = o.getString("label"),
        categoryId = o.getLong("categoryId"),
        note = o.optStringOrNull("note"),
        paidAmount = o.optLongOrNull("paidAmount"),
        merchant = o.optStringOrNull("merchant"),
        tags = o.optStringOrNull("tags"),
        sortOrder = o.getDouble("sortOrder"),
    )
}

class BackupException(message: String) : Exception(message)

fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key) || !has(key)) null else getLong(key)

fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key) || !has(key)) null else getString(key)

suspend fun createBackup(db: SpendrDatabase): Triple<String, ByteArray, Int> {
    val categories = db.categoryDao().list()
    val transactions = db.transactionDao().listAllForBackup()
    val quickAdds = db.quickAddDao().listForBackup()
    val receiptItems = db.receiptItemDao().listAllForBackup()

    val manifest = BackupArchive.buildManifest(categories.size, transactions.size, quickAdds.size)
    val categoriesJson = JSONArray().apply { categories.forEach { put(BackupArchive.categoryToJson(it)) } }
    val transactionsJson = JSONArray().apply { transactions.forEach { put(BackupArchive.transactionToJson(it)) } }
    val quickAddJson = JSONArray().apply { quickAdds.forEach { put(BackupArchive.quickAddToJson(it)) } }
    val receiptItemsJson = JSONArray().apply { receiptItems.forEach { put(BackupArchive.receiptItemToJson(it)) } }
    val bytes = BackupArchive.encode(manifest, categoriesJson, transactionsJson, quickAddJson, receiptItemsJson)
    return Triple(BackupArchive.archiveFileName(manifest.getLong("createdAt")), bytes, transactions.size)
}

suspend fun restoreBackup(db: SpendrDatabase, bytes: ByteArray): Int {
    val backup = BackupArchive.decode(bytes)
    BackupArchive.validate(backup)

    val categories = backup.getJSONArray("categories")
    val transactions = backup.getJSONArray("transactions")
    val quickAdds = backup.getJSONArray("quickAdd")
    val receiptItems = backup.optJSONArray("receiptItems") ?: JSONArray()

    // Real RN databases can hold orphan rows (transactions whose category was
    // deleted while FK enforcement was off on that connection), and the RN
    // restore replays them verbatim. Room enforces FKs, so lift them for the
    // restore the same way RN's connection effectively did. The pragma cannot
    // change inside a transaction, so it is set around it.
    val database = db.openHelper.writableDatabase
    database.query("PRAGMA foreign_keys = OFF").use { it.moveToFirst() }
    try {
        db.withTransaction {
        val sql = db.openHelper.writableDatabase
        sql.execSQL("DELETE FROM quick_add")
        sql.execSQL("DELETE FROM receipt_items")
        sql.execSQL("DELETE FROM transactions")
        sql.execSQL("DELETE FROM note_stats")
        sql.execSQL("DELETE FROM merchants")
        sql.execSQL("DELETE FROM categories")
        sql.execSQL(
            "DELETE FROM sqlite_sequence WHERE name IN ('transactions', 'receipt_items', 'note_stats', 'merchants', 'quick_add', 'categories')",
        )

        for (i in 0 until categories.length()) {
            val c = BackupArchive.categoryFromJson(categories.getJSONObject(i))
            sql.execSQL(
                "INSERT INTO categories (id, name, icon, color, sort_order, is_default, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(c.id, c.name, c.icon, c.color, c.sortOrder, if (c.isDefault) 1L else 0L, c.createdAt, c.updatedAt),
            )
        }
        for (i in 0 until transactions.length()) {
            val t = BackupArchive.transactionFromJson(transactions.getJSONObject(i))
            sql.execSQL(
                """
                INSERT INTO transactions (id, paid_amount, original_amount, discount_amount, discount_type,
                    category_id, note, merchant, tags, date, created_at, updated_at, type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    t.id, t.paidAmount, t.originalAmount, t.discountAmount, t.discountType,
                    t.categoryId, t.note, t.merchant, t.tags, t.date, t.createdAt, t.updatedAt, t.type,
                ),
            )
        }
        for (i in 0 until receiptItems.length()) {
            val item = BackupArchive.receiptItemFromJson(receiptItems.getJSONObject(i))
            sql.execSQL(
                "INSERT INTO receipt_items (id, transaction_id, name, paid_amount, quantity, sort_order) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf(item.id, item.transactionId, item.name, item.paidAmount, item.quantity, item.sortOrder),
            )
        }
        for (i in 0 until quickAdds.length()) {
            val q = BackupArchive.quickAddFromJson(quickAdds.getJSONObject(i))
            sql.execSQL(
                "INSERT INTO quick_add (id, label, category_id, note, paid_amount, merchant, tags, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(q.id, q.label, q.categoryId, q.note, q.paidAmount, q.merchant, q.tags, q.sortOrder),
            )
        }

        // Derived tables rebuild from restored transactions
        sql.execSQL(
            """
            INSERT OR IGNORE INTO merchants (name, created_at, updated_at)
            SELECT merchant, MIN(created_at), MAX(updated_at) FROM transactions
            WHERE merchant IS NOT NULL AND TRIM(merchant) != '' GROUP BY merchant
            """.trimIndent(),
        )
        sql.execSQL(
            """
            INSERT INTO note_stats (note, paid_amount, use_count, latest_date)
            SELECT note, paid_amount, COUNT(*), MAX(date) FROM transactions
            WHERE note IS NOT NULL AND note != '' AND paid_amount > 0 GROUP BY note, paid_amount
            """.trimIndent(),
        )
        }
    } finally {
        database.query("PRAGMA foreign_keys = ON").use { it.moveToFirst() }
    }
    return transactions.length()
}

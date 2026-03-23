package com.spetsian.pmacalculator.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class HistoryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun ensureSignedInAnonymously(): Result<Unit> {
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveHistory(expression: String, result: String): Result<Unit> {
        return try {
            // Важно: initFirebaseAuth() в Activity идёт параллельно — без этого сохранение
            // может вызваться до завершения signInAnonymously().
            ensureSignedInAnonymously().getOrElse { return Result.failure(it) }

            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User is not signed in"))

            if (!isValidExpression(expression) || !isValidResult(result)) {
                return Result.success(Unit)
            }

            val payload = hashMapOf(
                "expression" to expression,
                "result" to result,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .collection("history")
                .add(payload)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadHistory(limit: Long = 100): Result<List<HistoryItem>> {
        return try {
            ensureSignedInAnonymously().getOrElse { return Result.failure(it) }

            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User is not signed in"))

            // Без orderBy: документы без поля createdAt не «пропадают» из выборки (Firestore
            // исключает такие документы из запросов с orderBy). Сортировка на клиенте.
            val snapshot = db.collection("users")
                .document(uid)
                .collection("history")
                .get()
                .await()

            val items = snapshot.documents
                .map { doc ->
                    HistoryItem(
                        id = doc.id,
                        expression = doc.getString("expression").orEmpty(),
                        result = doc.getString("result").orEmpty(),
                        createdAt = doc.readCreatedAtMillis()
                    )
                }
                .sortedByDescending { it.createdAt }
                .take(limit.toInt().coerceIn(1, 500))

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidExpression(expression: String): Boolean {
        return expression.isNotBlank()
    }

    private fun isValidResult(result: String): Boolean {
        if (result.isBlank()) return false
        if (result == "Ошибка") return false

        val asDouble = result.toDoubleOrNull() ?: return true
        return !asDouble.isNaN() && !asDouble.isInfinite()
    }
}

private fun DocumentSnapshot.readCreatedAtMillis(): Long {
    getLong("createdAt")?.let { return it }
    getDouble("createdAt")?.let { return it.toLong() }
    getTimestamp("createdAt")?.let { return it.toDate().time }
    return 0L
}
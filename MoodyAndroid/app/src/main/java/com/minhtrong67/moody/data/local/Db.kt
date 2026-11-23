package com.minhtrong67.moody.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.EntryData
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val listJson = Json { ignoreUnknownKeys = true }
private val stringList = ListSerializer(String.serializer())

@Entity(tableName = "entries", indices = [Index("date")])
data class EntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val time: String,
    val mood: String,
    val score: Int,
    val activities: String,   // JSON array of activity ids
    val note: String,
) {
    fun toEntry() = Entry(
        id = id,
        date = date,
        time = time,
        mood = mood,
        score = score,
        activities = runCatching { listJson.decodeFromString(stringList, activities) }.getOrDefault(emptyList()),
        note = note,
    )
}

fun EntryData.toEntity(id: String) = EntryEntity(
    id = id,
    date = date,
    time = time,
    mood = mood,
    score = score,
    activities = listJson.encodeToString(stringList, activities),
    note = note,
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries")
    suspend fun getAll(): List<EntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<EntryEntity>)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [EntryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}

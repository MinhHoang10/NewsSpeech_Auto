package com.newsspeech.auto.database

import android.content.Context
import androidx.room.*
import com.newsspeech.auto.model.NewsArticle
import kotlinx.coroutines.flow.Flow

/**
 * DAO - Data Access Object
 */
@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles ORDER BY timestamp DESC")
    fun getAllNews(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY timestamp DESC")
    fun getNewsByCategory(category: String): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE source = :source ORDER BY timestamp DESC")
    fun getNewsBySource(source: String): Flow<List<NewsArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(news: List<NewsArticle>)

    @Query("DELETE FROM news_articles")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getCount(): Int

    @Query("DELETE FROM news_articles WHERE timestamp < :timestamp")
    suspend fun deleteOldNews(timestamp: String)
}

/**
 * Room Database
 */
@Database(
    entities = [NewsArticle::class],
    version = 1,
    exportSchema = false
)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao

    companion object {
        @Volatile
        private var INSTANCE: NewsDatabase? = null

        fun getDatabase(context: Context): NewsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NewsDatabase::class.java,
                    "news_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
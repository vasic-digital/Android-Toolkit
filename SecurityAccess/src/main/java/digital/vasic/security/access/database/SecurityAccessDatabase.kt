package digital.vasic.security.access.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import digital.vasic.security.access.BuildConfig
import digital.vasic.security.access.data.SecuritySettings
import digital.vasic.security.access.data.AccessAttempt
import digital.vasic.security.access.data.AccessCredential
import digital.vasic.security.access.data.AccessSession
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        SecuritySettings::class,
        AccessCredential::class,
        AccessAttempt::class,
        AccessSession::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SecurityAccessTypeConverters::class)
abstract class SecurityAccessDatabase : RoomDatabase() {

    abstract fun securitySettingsDao(): SecuritySettingsDao
    abstract fun accessCredentialDao(): AccessCredentialDao
    abstract fun accessAttemptDao(): AccessAttemptDao
    abstract fun accessSessionDao(): AccessSessionDao

    companion object {
        @Volatile
        private var INSTANCE: SecurityAccessDatabase? = null

        fun getInstance(context: Context): SecurityAccessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): SecurityAccessDatabase {
            val passphrase = SQLiteDatabase.getBytes(BuildConfig.DATABASE_PASSWORD.toCharArray())
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                SecurityAccessDatabase::class.java,
                BuildConfig.DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
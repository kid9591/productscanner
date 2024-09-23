package com.kid.productscanner.repository.cache.room.di

import android.app.Application
import androidx.room.Room
import com.kid.productscanner.repository.cache.room.ScannerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ScannerDbModule {

    @Provides
    fun provideScannerDatabase(application: Application): ScannerDatabase {
        return Room.databaseBuilder(
            application.applicationContext,
            ScannerDatabase::class.java, "scanner_db"
        ).build()
    }
}
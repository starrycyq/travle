package com.travle.app.data.database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTravelDatabase(@ApplicationContext context: Context): TravelDatabase {
        return TravelDatabase.getDatabase(context)
    }

    @Provides
    fun provideCollectionDao(database: TravelDatabase): CollectionDao {
        return database.collectionDao()
    }

    @Provides
    fun provideUserPreferenceDao(database: TravelDatabase): UserPreferenceDao {
        return database.userPreferenceDao()
    }
}
package com.example.intellipatassignment.di

import android.content.Context
import androidx.room.Room
import com.example.intellipatassignment.BuildConfig
import com.example.intellipatassignment.core.AndroidConnectivityObserver
import com.example.intellipatassignment.core.ConnectivityObserver
import com.example.intellipatassignment.data.local.AppDatabase
import com.example.intellipatassignment.data.local.CourseLocalStore
import com.example.intellipatassignment.data.local.RoomCourseLocalStore
import com.example.intellipatassignment.data.remote.AuthInterceptor
import com.example.intellipatassignment.data.remote.ConnectivityInterceptor
import com.example.intellipatassignment.data.remote.CourseApi
import com.example.intellipatassignment.data.remote.DebugLoggingInterceptor
import com.example.intellipatassignment.data.remote.MockApiInterceptor
import com.example.intellipatassignment.data.remote.MockServerPrefs
import com.example.intellipatassignment.data.repository.CourseRepositoryImpl
import com.example.intellipatassignment.data.repository.MockAuthRepository
import com.example.intellipatassignment.data.session.KeystoreSessionStorage
import com.example.intellipatassignment.data.session.SessionStorage
import com.example.intellipatassignment.domain.repository.AuthRepository
import com.example.intellipatassignment.domain.repository.CourseRepository
import com.example.intellipatassignment.domain.repository.SyncScheduler
import com.example.intellipatassignment.domain.usecase.LogoutUseCase
import com.example.intellipatassignment.ui.courses.CoursesViewModel
import com.example.intellipatassignment.ui.details.CourseDetailsViewModel
import com.example.intellipatassignment.ui.login.LoginViewModel
import com.example.intellipatassignment.work.SyncWorker
import com.example.intellipatassignment.work.WorkManagerSyncScheduler
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val dataModule = module {
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
    single<SessionStorage> { KeystoreSessionStorage(androidContext()) }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    single {
        OkHttpClient.Builder()
            .apply { if (BuildConfig.DEBUG) addInterceptor(DebugLoggingInterceptor()) }
            .addInterceptor(ConnectivityInterceptor(get()))
            .addInterceptor(AuthInterceptor(get()))
            .apply {
                if (BuildConfig.USE_MOCK_BACKEND) {
                    val prefs = androidContext().getSharedPreferences(MockServerPrefs.NAME, Context.MODE_PRIVATE)
                    addInterceptor(MockApiInterceptor(androidContext().assets, prefs))
                }
            }
            .build()
    }
    single { Json { ignoreUnknownKeys = true } }
    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<CourseApi> { get<Retrofit>().create(CourseApi::class.java) }

    single<SyncScheduler> { WorkManagerSyncScheduler(androidContext()) }
    single<CourseLocalStore> { RoomCourseLocalStore(get()) }
    single<CourseRepository> { CourseRepositoryImpl(get(), get(), get()) }
    single<AuthRepository> { MockAuthRepository(get()) }
    factory { LogoutUseCase(get(), get(), get()) }
}

val workModule = module {
    workerOf(::SyncWorker)
}

val presentationModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { CoursesViewModel(get(), get(), get()) }
    viewModel { (courseId: Int) -> CourseDetailsViewModel(courseId, get()) }
}

val appModules = listOf(dataModule, workModule, presentationModule)

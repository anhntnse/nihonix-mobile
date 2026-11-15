package com.nihonix.mobile.core.offline

import android.content.Context
import androidx.work.*
import com.nihonix.mobile.core.di.ServiceLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncScheduler(private val context:Context,private val environment:String) {
    private val work get()=WorkManager.getInstance(context)
    private fun request(account:String)=OneTimeWorkRequestBuilder<SyncWorker>()
        .setInputData(workDataOf("account" to account,"environment" to environment))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).addTag("nihonix-sync-$environment-$account").build()
    fun schedule(account:String) {
        work.enqueueUniqueWork("nihonix-sync-$environment-$account",ExistingWorkPolicy.APPEND_OR_REPLACE,request(account))
    }
    fun periodic(account:String) {
        work.enqueueUniquePeriodicWork("nihonix-periodic-$environment-$account",ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(15,TimeUnit.MINUTES)
                .setInputData(workDataOf("account" to account,"environment" to environment))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag("nihonix-sync-$environment-$account").build())
    }
    fun cancel(account:String) { work.cancelAllWorkByTag("nihonix-sync-$environment-$account") }
}
class SyncWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        if(inputData.getString("environment")!=ServiceLocator.environmentKey) return@withContext Result.success()
        val account=inputData.getString("account").orEmpty()
        try {
            if(account.isNotEmpty()) {
                if(ServiceLocator.progressRepository.account()!=account) return@withContext Result.success()
                suspend fun producer(call:suspend ()->Boolean):Boolean = try { call() }
                    catch(cancelled:CancellationException) { throw cancelled }
                    catch(_:Exception) { false }
                val progress=producer { ServiceLocator.progressRepository.sync(account) }
                val flashcards=producer { ServiceLocator.flashcardsRepository.sync(account) }
                val practice=producer { ServiceLocator.practiceRepository.sync(account) }
                if(!progress || !flashcards || !practice) return@withContext if(runAttemptCount<8) Result.retry() else Result.failure()
            }
            ServiceLocator.catalogSync.refresh()
            Result.success()
        } catch(cancelled:CancellationException) { throw cancelled }
        catch(_:Exception) { if(runAttemptCount<8) Result.retry() else Result.failure() }
    }
}
class CatalogSync(private val client:okhttp3.OkHttpClient,private val store:CatalogStore,private val baseUrl:String) {
    fun refresh() {
        val manifest=client.newCall(okhttp3.Request.Builder().url(baseUrl+"catalog/manifest")
            .header(com.nihonix.mobile.core.network.ApiConstants.HEADER_NO_AUTH,"true").build()).execute().use { response->
            check(response.isSuccessful && response.header("X-Nihonix-Cache")!="offline")
            response.body!!.string()
        }
        pruneCatalog(store,manifest)
        store.all().filterNot{it.url.endsWith("/catalog/manifest")}.sortedBy{it.checkedAt}.take(30).forEach { saved->
            client.newCall(okhttp3.Request.Builder().url(saved.url)
                .header(com.nihonix.mobile.core.network.ApiConstants.HEADER_NO_AUTH,"true").build()).execute().use { response->
                if(response.isSuccessful) response.body?.string()
            }
        }
    }
}

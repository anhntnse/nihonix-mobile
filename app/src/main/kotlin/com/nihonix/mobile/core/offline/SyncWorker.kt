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
        if(account.isEmpty()) return@withContext Result.success()
        try {
            if(account.isNotEmpty()) {
                if(ServiceLocator.progressRepository.account()!=account) return@withContext Result.success()
                suspend fun producer(call:suspend ()->Boolean):Boolean = try { call() }
                    catch(cancelled:CancellationException) { throw cancelled }
                    catch(_:Exception) { false }
                val progress=producer { ServiceLocator.progressRepository.sync(account) }
                val flashcards=producer { ServiceLocator.flashcardsRepository.sync(account) }
                val practice=producer { ServiceLocator.practiceRepository.sync(account) }
                val activity=producer { ServiceLocator.activityRepository.sync(account) }
                producer { ServiceLocator.notifications.reconcile() }
                if(!progress || !flashcards || !practice || !activity) return@withContext if(runAttemptCount<8) Result.retry() else Result.failure()
            }
            if(ServiceLocator.progressRepository.account()!=account) return@withContext Result.success()
            ServiceLocator.catalogSync.refresh()
            Result.success()
        } catch(cancelled:CancellationException) { throw cancelled }
        catch(_:Exception) { if(runAttemptCount<8) Result.retry() else Result.failure() }
    }
}
class CatalogSync(private val client:okhttp3.OkHttpClient,private val store:CatalogStore,private val baseUrl:String,
    private val currentSession:()->com.nihonix.mobile.core.network.interceptor.SessionRequest?) {
    fun refresh() {
        val session=currentSession()?:return
        fun ensureSession() { check(currentSession()==session) { "Login changed during catalog sync." } }
        val manifest=client.newCall(okhttp3.Request.Builder().url(baseUrl+"catalog/manifest")
            .tag(com.nihonix.mobile.core.network.interceptor.SessionRequest::class.java,session).build()).execute().use { response->
            check(response.isSuccessful && response.header("X-Nihonix-Cache")!="offline")
            response.body!!.string()
        }
        synchronized(store) { ensureSession();pruneCatalog(store,manifest) }
        val catalogUrl=Regex("^"+Regex.escape(baseUrl)+"(catalog|levels|topics)(/|\\?|$)")
        store.all().filter{catalogUrl.containsMatchIn(it.url)}
            .filterNot{it.url.endsWith("/catalog/manifest")}.sortedBy{it.checkedAt}.take(30).forEach { saved->
            ensureSession()
            client.newCall(okhttp3.Request.Builder().url(saved.url)
                .tag(com.nihonix.mobile.core.network.interceptor.SessionRequest::class.java,session).build()).execute().use { response->
                if(response.isSuccessful) response.body?.string()
            }
        }
    }
}

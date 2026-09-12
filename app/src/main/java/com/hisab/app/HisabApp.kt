package com.hisab.app

import android.app.Application
import com.hisab.app.data.remote.SyncWorker

class HisabApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // autoSyncEnabled ডিফল্ট true, তাই প্রথমবার অ্যাপ চালু হলেই background sync শুরু হয়ে
        // যায় — Settings-এ গিয়ে আলাদা করে চালু করতে হয় না।
        if (container.syncPrefs.autoSyncEnabled) SyncWorker.enable(this)
    }
}

package com.nihonix.mobile

import android.app.Application
import com.nihonix.mobile.core.di.ServiceLocator

class NihonixApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}

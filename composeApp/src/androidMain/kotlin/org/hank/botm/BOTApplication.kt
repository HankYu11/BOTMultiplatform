package org.hank.botm

import android.app.Application
import org.hank.botm.di.initKoin
import org.koin.android.ext.koin.androidContext

class BOTApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@BOTApplication)
        }
    }
}
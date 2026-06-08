package com.multica.app

import android.app.Application
import android.util.Log
import com.multica.app.di.ServiceLocator

class MulticaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Log.i(TAG, "MulticaApp onCreate, Build.DEBUG=${BuildConfig.DEBUG}")
    }

    companion object {
        const val TAG = "MulticaApp"
    }
}

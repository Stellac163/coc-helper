package com.cochelper.app

import android.app.Application
import com.cochelper.app.di.AppContainer

class CocHelperApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

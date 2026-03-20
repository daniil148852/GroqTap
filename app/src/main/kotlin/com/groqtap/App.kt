package com.groqtap

import android.app.Application
import com.groqtap.data.GroqApi
import com.groqtap.data.Prefs

class App : Application() {

    lateinit var prefs: Prefs
        private set

    lateinit var groqApi: GroqApi
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        groqApi = GroqApi()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}

package ru.sweetbread.flake

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import org.acra.BuildConfig
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

class FlakeApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        DynamicColors.applyToActivitiesIfAvailable(this)

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON

            httpSender {
                uri = "https://flake.coders-squad.com/api/v1/android-bug-report"
            }
        }
    }
}
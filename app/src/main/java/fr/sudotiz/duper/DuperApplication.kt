package fr.sudotiz.duper

import android.app.Application
import fr.sudotiz.duper.data.PreferencesRepository

class DuperApplication : Application() {

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(this)
    }
}

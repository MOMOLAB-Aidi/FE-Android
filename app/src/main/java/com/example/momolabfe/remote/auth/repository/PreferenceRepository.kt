package com.example.momolabfe.remote.auth.repository

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

// SharedPreferences 또는 DataStore를 래핑하는 클래스

interface PreferenceRepository {
    fun getPatientId(): LiveData<String>
    suspend fun savePatientId(id: String)
    suspend fun clearPatientId()
}

class SharedPreferencesRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : PreferenceRepository {

    companion object {
        private const val KEY_PATIENT_ID = "patient_id_key"
    }

    // LiveData로 SharedPreferences의 값 변화를 관찰
    override fun getPatientId(): LiveData<String> {
        val liveData = MutableLiveData<String>()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == KEY_PATIENT_ID) {
                liveData.postValue(sharedPrefs.getString(KEY_PATIENT_ID, ""))
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        liveData.value = sharedPreferences.getString(KEY_PATIENT_ID, "")

        return liveData
    }

    override suspend fun savePatientId(id: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_PATIENT_ID, id)
            apply()
        }
    }

    override suspend fun clearPatientId() {
        with(sharedPreferences.edit()) {
            remove(KEY_PATIENT_ID)
            apply()
        }
    }
}
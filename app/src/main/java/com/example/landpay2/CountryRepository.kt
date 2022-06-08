package com.example.landpay2

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore

class CountryRepository {
    fun getResponseFromFirestoreUsingCoroutines() : MutableLiveData<Response> {
        val mutableLiveData = MutableLiveData<Response>()
        FirebaseFirestore.getInstance().collection(COUNTRY_KEY).get().addOnCompleteListener { task ->
            val response = Response()
            if (task.isSuccessful) {
                val result = task.result
                result?.let {
                    response.countries = it.documents.mapNotNull { snapShot ->
                        snapShot.toObject(Country::class.java)
                    }
                }
            } else {
                response.exception = task.exception
            }
            mutableLiveData.value = response
        }
        return mutableLiveData

    }

    companion object {
        const val COUNTRY_KEY = "Country"
    }
}
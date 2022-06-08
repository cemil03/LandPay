package com.example.landpay2

import androidx.lifecycle.ViewModel

class CountriesViewModel: ViewModel() {
    var repository: CountryRepository? = null

    init {
        repository = CountryRepository()
    }

    fun getResponseUsingCallback() = repository?.getResponseFromFirestoreUsingCoroutines()

}
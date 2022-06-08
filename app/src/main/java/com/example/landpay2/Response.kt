package com.example.landpay2

import java.lang.Exception

data class Response (var countries:List<Country>? = null , var exception: Exception? = null)
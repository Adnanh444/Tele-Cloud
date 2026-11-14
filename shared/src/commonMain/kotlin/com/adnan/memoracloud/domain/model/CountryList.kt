package com.adnan.memoracloud.domain.model

data class CountryList(
    val countries: List<Country>,
    val detected: Country?
)

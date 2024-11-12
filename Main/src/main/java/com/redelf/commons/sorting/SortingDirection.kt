package com.redelf.commons.sorting

enum class SortingDirection(private val direction: String) {

    ASCENDING("ASC"),
    DESCENDING("DESC");

    companion object {

        fun fromString(direction: String): SortingDirection? {

            SortingDirection.entries.forEach {

                if (it.direction == direction) {

                    return it
                }
            }

            return null
        }
    }
}
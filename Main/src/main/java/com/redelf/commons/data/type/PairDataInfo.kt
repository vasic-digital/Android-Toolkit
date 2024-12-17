package com.redelf.commons.data.type

data class PairDataInfo(

    var first: Any? = null,
    var second: Any? = null,
    var firstType: String? = first?.javaClass?.canonicalName,
    var secondType: String? = second?.javaClass?.canonicalName

) {

    constructor() : this(null, null, null, null)
}

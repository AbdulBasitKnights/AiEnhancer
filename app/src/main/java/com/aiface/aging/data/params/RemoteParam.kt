package com.aiface.aging.data.params

sealed class RemoteParam {
    sealed class StringValue(val value: String)
    sealed class BoolValue(val value: Boolean)
    sealed class LongValue(val value: Long)
    sealed class DoubleValue(val value: Double)
}
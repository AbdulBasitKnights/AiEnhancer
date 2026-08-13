package com.aiface.aging.features.share

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class ExtrasShareImageActivity(
    var id: Long? = null,
    var uri: Uri? = null,
    var displayName: String? = null,
    var path: String? = null,
    var fromMyWork: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readParcelable(Uri::class.java.classLoader),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeParcelable(uri, flags)
        parcel.writeString(displayName)
        parcel.writeString(path)
        parcel.writeByte(if (fromMyWork) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ExtrasShareImageActivity> {
        override fun createFromParcel(parcel: Parcel): ExtrasShareImageActivity {
            return ExtrasShareImageActivity(parcel)
        }

        override fun newArray(size: Int): Array<ExtrasShareImageActivity?> {
            return arrayOfNulls(size)
        }
    }
}
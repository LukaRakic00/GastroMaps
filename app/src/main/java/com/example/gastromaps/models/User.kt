package com.example.gastromaps.activity

import android.os.Parcel
import android.os.Parcelable

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobile: String = "",
    val fcmToken: String = "" // token User-a


): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) = with(parcel){
        writeString(id)
        writeString(fullName)
        writeString(email)
        writeString(mobile)
        writeString(fcmToken)
    }

    override fun describeContents() = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<User> = object : Parcelable.Creator<User> {
            override fun createFromParcel(source: Parcel): User = User(source)
            override fun newArray(size: Int): Array<User?> = arrayOfNulls(size)
        }
    }
}
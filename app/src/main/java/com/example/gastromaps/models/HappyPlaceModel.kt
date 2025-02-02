package com.example.gastromaps.models

import android.os.Parcel
import android.os.Parcelable

data class HappyPlaceModel(
    val id: String = "",
    val title: String = "",
    val image: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(image)
        parcel.writeString(description)
        parcel.writeString(date)
        parcel.writeString(location)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    // Helper functions for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "image" to image,
            "description" to description,
            "date" to date,
            "location" to location,
            "latitude" to latitude,
            "longitude" to longitude
        )
    }

    companion object CREATOR : Parcelable.Creator<HappyPlaceModel> {
        override fun createFromParcel(parcel: Parcel): HappyPlaceModel {
            return HappyPlaceModel(parcel)
        }

        override fun newArray(size: Int): Array<HappyPlaceModel?> {
            return arrayOfNulls(size)
        }

        // Added Firestore helper function here
        fun fromMap(map: Map<String, Any>): HappyPlaceModel {
            return HappyPlaceModel(
                id = map["id"] as String,
                title = map["title"] as String,
                image = map["image"] as String,
                description = map["description"] as String,
                date = map["date"] as String,
                location = map["location"] as String,
                latitude = (map["latitude"] as Number).toDouble(),
                longitude = (map["longitude"] as Number).toDouble()
            )
        }
    }
}
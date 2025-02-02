package com.example.gastromaps.utils

import android.app.Activity
import android.net.Uri
import android.webkit.MimeTypeMap

object Constants {

    // Firebase kolekcije
    const val USERS: String = "users"
    const val TASK_LIST: String = "taskList"
    const val HAPPY_PLACES: String = "happyPlaces"

    // Imena polja za Users kolekciju
    const val FULLNAME: String = "fullname"
    const val EMAIL: String = "email"
    const val MOBILE: String = "mobile"

    // Imena polja za Happy Places kolekciju
    const val PLACE_TITLE: String = "title"
    const val PLACE_IMAGE: String = "image"
    const val PLACE_DESCRIPTION: String = "description"
    const val PLACE_DATE: String = "date"
    const val PLACE_LOCATION: String = "location"
    const val PLACE_LATITUDE: String = "latitude"
    const val PLACE_LONGITUDE: String = "longitude"

    // Storage paths za slike
    const val PLACE_IMAGE_PATH: String = "place_images"

    // Permission codes
    const val READ_STORAGE_PERMISSION_CODE = 1
    const val PICK_IMAGE_REQUEST_CODE = 2
    const val PLACE_AUTOCOMPLETE_REQUESTCODE = 3


    fun getFileExtension(activity: Activity, uri: Uri?): String? {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(activity.contentResolver.getType(uri!!))
    }

    fun getImageFileName(userId: String, extension: String?): String {
        return "PLACE_${userId}_${System.currentTimeMillis()}.${extension}"
    }
}
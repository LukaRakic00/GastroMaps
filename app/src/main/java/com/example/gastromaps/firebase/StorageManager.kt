package com.example.gastromaps.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.gastromaps.utils.Constants
import com.google.firebase.Firebase
import com.google.firebase.storage.storage

class StorageManager(private val context: Context) {
    private val storage = Firebase.storage

    fun uploadImage(
        imageUri: Uri,
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            Log.d("StorageManager", "Starting image upload. URI: $imageUri")
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val imageBytes = inputStream?.readBytes()
            inputStream?.close()

            if (imageBytes != null) {
                val fileName = Constants.getImageFileName(userId, "jpg")
                Log.d("StorageManager", "Uploading to Firebase. Filename: $fileName")

                val ref = storage.reference
                    .child(Constants.PLACE_IMAGE_PATH)
                    .child(fileName)

                ref.putBytes(imageBytes)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        // Get the download URL
                        ref.downloadUrl
                    }
                    .addOnSuccessListener { uri ->
                        Log.d("StorageManager", "Upload successful. Download URL: $uri")
                        onSuccess(uri.toString())  // Make sure this is actually a valid URL
                    }
                    .addOnFailureListener { e ->
                        Log.e("StorageManager", "Upload failed", e)
                        onFailure(e)
                    }
            } else {
                onFailure(Exception("Failed to read image data"))
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Error in uploadImage", e)
            onFailure(e)
        }
    }
}
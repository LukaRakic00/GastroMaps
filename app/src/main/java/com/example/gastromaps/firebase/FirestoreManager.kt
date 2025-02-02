package com.example.gastromaps.firebase

import android.util.Log
import com.example.gastromaps.activity.SignUpActivity
import com.example.gastromaps.activity.User
import com.example.gastromaps.models.HappyPlaceModel
import com.example.gastromaps.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreManager {
    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }.addOnFailureListener { e ->
                Log.e(
                    activity.javaClass.simpleName,
                    "Error writing document", e
                )
            }
    }

    fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid ?: ""
    }

    fun addHappyPlace(happyPlace: HappyPlaceModel, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            onFailure(Exception("User not logged in"))
            return
        }

        val happyPlaceData = hashMapOf(
            Constants.PLACE_TITLE to happyPlace.title,
            Constants.PLACE_IMAGE to happyPlace.image,
            Constants.PLACE_DESCRIPTION to happyPlace.description,
            Constants.PLACE_DATE to happyPlace.date,
            Constants.PLACE_LOCATION to happyPlace.location,
            Constants.PLACE_LATITUDE to happyPlace.latitude,
            Constants.PLACE_LONGITUDE to happyPlace.longitude
        )

        mFireStore.collection(Constants.USERS)
            .document(currentUserId)
            .collection(Constants.HAPPY_PLACES)
            .add(happyPlaceData)
            .addOnSuccessListener { documentRef ->
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getUserHappyPlaces(onSuccess: (List<HappyPlaceModel>) -> Unit, onFailure: (Exception) -> Unit) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .collection(Constants.HAPPY_PLACES)
            .get()
            .addOnSuccessListener { documents ->
                val happyPlaces = ArrayList<HappyPlaceModel>()

                for (doc in documents) {
                    val place = HappyPlaceModel(
                        id = doc.id,
                        title = doc.getString(Constants.PLACE_TITLE) ?: "",
                        image = doc.getString(Constants.PLACE_IMAGE) ?: "",
                        description = doc.getString(Constants.PLACE_DESCRIPTION) ?: "",
                        date = doc.getString(Constants.PLACE_DATE) ?: "",
                        location = doc.getString(Constants.PLACE_LOCATION) ?: "",
                        latitude = doc.getDouble(Constants.PLACE_LATITUDE) ?: 0.0,
                        longitude = doc.getDouble(Constants.PLACE_LONGITUDE) ?: 0.0
                    )
                    happyPlaces.add(place)
                }
                onSuccess(happyPlaces)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateHappyPlace(happyPlace: HappyPlaceModel, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val happyPlaceData = happyPlace.toMap()

        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .collection(Constants.HAPPY_PLACES)
            .document(happyPlace.id)
            .update(happyPlaceData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun deleteHappyPlace(placeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .collection(Constants.HAPPY_PLACES)
            .document(placeId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
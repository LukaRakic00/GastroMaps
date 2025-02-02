package com.example.gastromaps.activity

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.gastromaps.R
import com.example.gastromaps.databinding.ActivityAddHappyPlaceBinding
import com.example.gastromaps.firebase.FirestoreManager
import com.example.gastromaps.firebase.StorageManager
import com.example.gastromaps.models.HappyPlaceModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class AddHappyPlaceActivity : BaseActivity(), View.OnClickListener {

    private var cal = Calendar.getInstance()
    private lateinit var firestoreManager: FirestoreManager
    private lateinit var storageManager: StorageManager
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var galleryImageResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var resultLauncherCamera: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityAddHappyPlaceBinding
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
    private var isImageUploading = false
    private var editPlaceDetails: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestoreManager = FirestoreManager()
        storageManager = StorageManager(this)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerOnActivityForResult()

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            editPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
            populateExistingData()
        }


        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAddPlace.setNavigationOnClickListener {
            onBackPressed()
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        setupLocationClick()

        binding.etDate.setOnClickListener(this)
        binding.tvAddImage.setOnClickListener(this)
        binding.btnSave.setOnClickListener(this)

        // Set current date by default
        if (editPlaceDetails == null) {
            updateDateInView()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    resultLauncherCamera.launch(cameraIntent)
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            mLatitude = data?.getDoubleExtra("lat", 0.0) ?: 0.0
            mLongitude = data?.getDoubleExtra("lng", 0.0) ?: 0.0
            val address = data?.getStringExtra("address") ?: ""
            binding.etLocation.setText(address)
        }
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                    "Capture photo from Camera")
                pictureDialog.setItems(pictureDialogItems){
                        _, which ->
                    when(which){
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save -> {
                if (!binding.btnSave.isEnabled || isImageUploading) {
                    return
                }
                when {
                    binding.etTitle.text.isNullOrEmpty() -> {
                        showErrorSnackBar("Please enter title")
                    }
                    binding.etDescription.text.isNullOrEmpty() -> {
                        showErrorSnackBar("Please enter description")
                    }
                    binding.etLocation.text.isNullOrEmpty() -> {
                        showErrorSnackBar("Please enter location")
                    }
                    saveImageToInternalStorage == null -> {
                        showErrorSnackBar("Please select and wait for image upload to complete")
                    }
                    else -> {
                        // Disable the save button immediately
                        binding.btnSave.isEnabled = false
                        showProgressDialog("Saving place...")

                        val happyPlace = if (editPlaceDetails != null) {
                            // In edit mode, keep the original ID
                            HappyPlaceModel(
                                id = editPlaceDetails!!.id,
                                title = binding.etTitle.text.toString(),
                                image = saveImageToInternalStorage.toString(),
                                description = binding.etDescription.text.toString(),
                                date = binding.etDate.text.toString(),
                                location = binding.etLocation.text.toString(),
                                latitude = mLatitude,
                                longitude = mLongitude
                            )
                        }else {
                            // In add mode, create new place
                            HappyPlaceModel(
                                title = binding.etTitle.text.toString(),
                                image = saveImageToInternalStorage.toString(),
                                description = binding.etDescription.text.toString(),
                                date = binding.etDate.text.toString(),
                                location = binding.etLocation.text.toString(),
                                latitude = mLatitude,
                                longitude = mLongitude
                            )
                        }
                        if (editPlaceDetails != null) {
                            firestoreManager.updateHappyPlace(
                                happyPlace,
                                onSuccess = {
                                    hideProgressDialog()
                                    Toast.makeText(
                                        this@AddHappyPlaceActivity,
                                        "Place updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                },
                                onFailure = { e ->
                                    hideProgressDialog()
                                    binding.btnSave.isEnabled = true
                                    showErrorSnackBar("Error updating place: ${e.message}")
                                }
                            )
                        }else{
                            firestoreManager.addHappyPlace(
                                happyPlace,
                                onSuccess = {
                                    hideProgressDialog()
                                    Toast.makeText(
                                        this@AddHappyPlaceActivity,
                                        "Place added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                },
                                onFailure = { e ->
                                    hideProgressDialog()
                                    binding.btnSave.isEnabled = true
                                    showErrorSnackBar("Error adding place: ${e.message}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun populateExistingData() {
        editPlaceDetails?.let { place ->
            binding.apply {
                etTitle.setText(place.title)
                etDescription.setText(place.description)
                etDate.setText(place.date)
                etLocation.setText(place.location)

                // Load the existing image
                saveImageToInternalStorage = Uri.parse(place.image)
                Glide.with(this@AddHappyPlaceActivity)
                    .load(place.image)
                    .into(ivPlaceImage)

                // Update toolbar title to indicate edit mode
                toolbarAddPlace.title = "Edit Place"
                btnSave.text = "Update"
            }
        }
    }

    private fun registerOnActivityForResult() {
        resultLauncherCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val thumbNail: Bitmap = result.data!!.extras?.get("data") as Bitmap
                binding.ivPlaceImage.setImageBitmap(thumbNail)

                isImageUploading = true
                binding.btnSave.isEnabled = false
                uploadImageToFirebase(thumbNail) { uri ->
                    isImageUploading = false
                    binding.btnSave.isEnabled = true
                    if (uri != null) {
                        saveImageToInternalStorage = uri
                        Log.e("Save image: ", "Path :: $saveImageToInternalStorage")
                    }
                }
            }
        }

        galleryImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                if (data != null) {
                    val contentUri = data.data
                    try {
                        val selectedImageBitmap: Bitmap = MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            contentUri
                        )
                        binding.ivPlaceImage.setImageBitmap(selectedImageBitmap)

                        isImageUploading = true
                        binding.btnSave.isEnabled = false
                        uploadImageToFirebase(selectedImageBitmap) { uri ->
                            isImageUploading = false
                            binding.btnSave.isEnabled = true
                            if (uri != null) {
                                saveImageToInternalStorage = uri
                                Log.e("Save image: ", "Path :: $saveImageToInternalStorage")
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncherCamera.launch(cameraIntent)
        }
    }

    private fun choosePhotoFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    GALLERY_PERMISSION_CODE
                )
            } else {
                openGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    GALLERY_PERMISSION_CODE
                )
            } else {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryImageResultLauncher.launch(galleryIntent)
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("" +
                "It looks like you turned off permission required " +
                "for this feature. It can be enabled under " +
                "the Application Settings")
            .setPositiveButton("GO TO SETTINGS")
            { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){ dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView() {
        val myFormat = "dd.MM.yyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding.etDate.setText(sdf.format(cal.time).toString())
    }

    private fun uploadImageToFirebase(bitmap: Bitmap, onComplete: (Uri?) -> Unit) {
        try {
            showProgressDialog("Uploading image...")

            // Save image locally
            val imageFile = saveImageToInternalStorage(bitmap)
            val contentUri = getImageContentUri(imageFile)

            storageManager.uploadImage(
                contentUri,
                firestoreManager.getCurrentUserId(),
                onSuccess = { downloadUrl ->
                    hideProgressDialog()
                    saveImageToInternalStorage = Uri.parse(downloadUrl)
                    onComplete(Uri.parse(downloadUrl))
                    Log.d("AddHappyPlace", "Image uploaded successfully: $downloadUrl")
                },
                onFailure = { e ->
                    hideProgressDialog()
                    Toast.makeText(
                        this@AddHappyPlaceActivity,
                        "Failed to upload image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete(null)
                    Log.e("AddHappyPlace", "Failed to upload image", e)
                }
            )
        } catch (e: Exception) {
            hideProgressDialog()
            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            onComplete(null)
            Log.e("AddHappyPlace", "Error in uploadImageToFirebase", e)
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): File {
        val directory = File(filesDir, "HappyPlaceImages")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "${UUID.randomUUID()}.jpg")

        try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }

        return file
    }

    private fun getImageContentUri(imageFile: File): Uri {
        return FileProvider.getUriForFile(
            applicationContext,
            "com.example.gastromaps.fileprovider",
            imageFile
        )
    }

    private fun setupLocationClick() {
        binding.etLocation.setOnClickListener {
            if (checkLocationPermissions()) {
                openMap()
            } else {
                requestLocationPermissions()
            }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun openMap() {
        val intent = Intent(this, MapActivity::class.java)
        startActivityForResult(intent, MAP_LOCATION_REQUEST_CODE)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val MAP_LOCATION_REQUEST_CODE = 2
        private const val CAMERA_PERMISSION_CODE = 100
        private const val GALLERY_PERMISSION_CODE = 101
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
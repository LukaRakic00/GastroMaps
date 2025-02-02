// SignUpActivity.kt - Aktivnost za registraciju
package com.example.gastromaps.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.gastromaps.databinding.ActivitySignUpBinding
import com.example.gastromaps.firebase.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : BaseActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        FirebaseFirestore.setLoggingEnabled(true)
        setupClickListeners()
    }

    fun userRegisteredSuccess() {
        Toast.makeText(
            this@SignUpActivity,
            "you have succesfully registered",
            Toast.LENGTH_SHORT
        ).show()
        hideProgressDialog()
        /**
         * Ovde se novi registrovani korisnik automatski prijavljuje
         * tako da samo odjavljujemo korisnika sa Firebase-a
         * i pošaljite ga na Intro Screen za prijavu
         * */
        FirebaseAuth.getInstance().signOut()
        // kraj finish Sign-Up Screen
        finish()
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val fullName = binding.edtSignUpFullName.text.toString().trim { it <= ' '}
            val email = binding.edtSignUpEmail.text.toString().trim { it <= ' '}
            val mobile = binding.edtSignUpMobile.text.toString().trim { it <= ' '}
            val password = binding.edtSignUpPassword.text.toString().trim { it <= ' '}
            val confirmPassword = binding.edtSignUpConfirmPassword.text.toString().trim { it <= ' '}

            if (validateInput(fullName, email, mobile, password, confirmPassword)) {
                showProgressDialog("Please Wait...")
                registerUser(fullName, email, mobile, password)
            }
        }

        binding.txtSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun registerUser(fullName: String, email: String, mobile: String, password: String) {

        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firebase registrovani user
                    val firebaseUser: FirebaseUser = task.result!!.user!!

                    // Registrovani email
                    val registeredEmail = firebaseUser.email!!
                    val user = User(firebaseUser.uid, fullName, registeredEmail, mobile)

                    FirestoreManager().registerUser(this, user)
                    val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@SignUpActivity,
                        "Registration failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun validateInput(
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            fullName.isEmpty() -> {
                showErrorSnackBar("Please enter full name")
                false
            }
            email.isEmpty() -> {
                showErrorSnackBar("Please enter email")
                false
            }
            mobile.isEmpty() -> {
                showErrorSnackBar("Please enter mobile number")
                false
            }
            password.isEmpty() -> {
                showErrorSnackBar("Please enter password")
                false
            }
            confirmPassword.isEmpty() -> {
                showErrorSnackBar("Please confirm your password")
                false
            }
            password != confirmPassword -> {
                showErrorSnackBar("Passwords don't match")
                false
            }
            else -> true
        }
    }
}
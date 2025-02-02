package com.example.gastromaps.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gastromaps.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.edtSignInEmail.text.toString()
            val password = binding.edtSignInPassword.text.toString()

            if (validateInput(email, password)) {
                showProgressBar()
                signInUser(email, password)
            }
        }

        binding.txtSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                hideProgressBar()
                Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show()
                // Navigate to main activity
                val intent = Intent(this@SignInActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProgressBar() {
        binding.signInProgressBar.visibility = View.VISIBLE
        binding.btnSignIn.visibility = View.INVISIBLE
    }

    private fun hideProgressBar() {
        binding.signInProgressBar.visibility = View.INVISIBLE
        binding.btnSignIn.visibility = View.VISIBLE
    }
}
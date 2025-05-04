package com.ryujinstudio.story

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var  idEmail: EditText
    private lateinit var  idPass: EditText
    private lateinit var idButtonIngresar: Button
    private lateinit var idBtnRegistrate: ImageButton
    private lateinit var idBtnRecuperar: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        requestLocationPermissions()

        auth = FirebaseAuth.getInstance()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            startActivity(Intent(this,Home::class.java))
            finish()
        }

        idEmail =  findViewById(R.id.idEmail)
        idPass = findViewById(R.id.idPass)
        idButtonIngresar =  findViewById(R.id.idButtonIngresar)
        idBtnRegistrate = findViewById(R.id.idBtnRegistrate)
        idBtnRecuperar = findViewById(R.id.idBtnRecuperar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        idButtonIngresar.setOnClickListener{loginUser()}
        idBtnRegistrate.setOnClickListener{goToRegister()}
        idBtnRecuperar.setOnClickListener{goToRecupPass()}
    }

    private fun loginUser(){
        val email = idEmail.text.toString()
        val password = idPass.text.toString()

        if(email.isEmpty() and password.isEmpty()){
            Toast.makeText(this,"Ingresa las Credenciales",Toast.LENGTH_LONG).show()
        }else{
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this){ task->
                    if (task.isSuccessful){
                        startActivity(Intent(this,Home::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private val LOCATION_AND_STORAGE_PERMISSION_REQUEST_CODE = 1

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), LOCATION_AND_STORAGE_PERMISSION_REQUEST_CODE)
        }
    }



    private fun goToRegister(){
        startActivity(Intent(this,Registro::class.java))
        finish()
    }

    private fun goToRecupPass(){
        startActivity(Intent(this,RecupPass::class.java))
        finish()
    }
}
package com.ryujinstudio.story

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class Registro : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var idEmail: EditText
    private lateinit var idPass: EditText
    private lateinit var idPassConfirm: EditText
    private lateinit var idButtonRegistrar: Button
    private lateinit var idBtnLogin: ImageButton
    private lateinit var db: FirebaseFirestore
    private lateinit var idUser: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        idEmail=findViewById(R.id.idEmail)
        idPass=findViewById(R.id.idPass)
        idPassConfirm=findViewById(R.id.idPassConfirm)
        idButtonRegistrar=findViewById(R.id.idButtonRegistrar)
        idBtnLogin=findViewById(R.id.idBtnLogin)
        idUser=findViewById(R.id.idUser)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        idBtnLogin.setOnClickListener{ goToLogin() }
        idButtonRegistrar.setOnClickListener{RegisterUser()}
    }

    private fun RegisterUser() {
        val email = idEmail.text.toString()
        val pass = idPass.text.toString()
        val passConfirm = idPassConfirm.text.toString()
        val userName = idUser.text.toString()
        val fechaActual = Timestamp.now()

        if (email.isNotEmpty() && pass.isNotEmpty() && passConfirm.isNotEmpty() && userName.isNotEmpty()) {
            if (pass == passConfirm) {
                // Primero registramos al usuario en Firebase Auth
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Si la creación fue exitosa, obtenemos el UID de Firebase
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Ahora registramos los datos del usuario en Firestore
                                val user = hashMapOf(
                                    "nombre" to userName,
                                    "correo" to email,
                                    "fechaRegistro" to fechaActual,
                                    "UID" to userId // Guarda el UID de Firebase, no el generado aleatoriamente
                                )

                                db.collection("Usuarios")
                                    .document(userId) // Usa el UID de Firebase para guardar el documento
                                    .set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                        // Redirige al MainActivity
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al guardar usuario: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(this, "Error al obtener UID", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val errorMsg = task.exception?.message
                            Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Ingresa todos los campos", Toast.LENGTH_LONG).show()
        }
    }


    private fun goToLogin(){
        startActivity(Intent(this,MainActivity::class.java))
        finish()
    }
}
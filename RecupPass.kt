package com.ryujinstudio.story

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

class RecupPass : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var idEmail: EditText
    private lateinit var idButtonRegistrar: Button
    private lateinit var idBtnLogin: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recup_pass)

        auth= FirebaseAuth.getInstance()

        idEmail=findViewById(R.id.idEmail)
        idButtonRegistrar=findViewById(R.id.idButtonRegistrar)
        idBtnLogin=findViewById(R.id.idBtnLogin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        idBtnLogin.setOnClickListener{goToHome()}
        idButtonRegistrar.setOnClickListener{recupPass()}
    }

    private fun goToHome(){
        startActivity(Intent(this,MainActivity::class.java))
        finish()
    }

    private fun recupPass(){
        val email = idEmail.text.toString()
        if (email.isNotEmpty()){
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this){task->
                    if (task.isSuccessful){
                        Toast.makeText(this, "Correo de recuperación enviado", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }else{
            Toast.makeText(this,"Ingresa Un Correo",Toast.LENGTH_LONG).show()
        }
    }
}
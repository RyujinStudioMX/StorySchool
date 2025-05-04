package com.ryujinstudio.story

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class Peticiones : AppCompatActivity() {

    private lateinit var idBtnLogin: ImageButton
    private lateinit var btnSubirInformacion: Button
    private lateinit var idAutor: TextView
    private lateinit var idLatitud: TextView
    private lateinit var idLongitud: TextView
    private lateinit var etNombre: TextInputEditText
    private lateinit var etTipo: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etInfoResumen: TextInputEditText
    private lateinit var etInfoCompleta: TextInputEditText
    private lateinit var btnSeleccionarImagen: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var uploadedImageUrl: String? = null
    private var subidaEnProgreso = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_peticiones)

        idBtnLogin = findViewById(R.id.idBtnLogin)
        btnSubirInformacion = findViewById(R.id.btnSubirInformacion)
        idAutor = findViewById(R.id.idAutor)
        idLatitud = findViewById(R.id.idLatitud)
        idLongitud = findViewById(R.id.idLongitud)
        etNombre = findViewById(R.id.etNombre)
        etTipo = findViewById(R.id.etTipo)
        etCategoria = findViewById(R.id.etCategoria)
        etInfoResumen = findViewById(R.id.etInfoResumen)
        etInfoCompleta = findViewById(R.id.etInfoCompleta)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnSubirInformacion.isEnabled = true // Ahora está habilitado desde el inicio

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        InfoUser()
        locUser()

        idBtnLogin.setOnClickListener { goToHome() }
        btnSeleccionarImagen.setOnClickListener { selectIMG() }

        btnSubirInformacion.setOnClickListener {
            if (uploadedImageUrl == null) {
                Toast.makeText(this, "Espera, la imagen aún se está subiendo...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (subidaEnProgreso) {
                Toast.makeText(this, "Ya se está procesando una subida...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RegisterPoint()
        }
    }

    private fun InfoUser() {
        val user = auth.currentUser
        user?.email?.let { email ->
            db.collection("Usuarios").whereEqualTo("correo", email).get()
                .addOnSuccessListener { result ->
                    idAutor.text = result.documents.firstOrNull()?.getString("nombre") ?: "Desconocido"
                }
                .addOnFailureListener {
                    idAutor.text = "Error obteniendo datos"
                    Log.e("InfoUser", it.message ?: "Error desconocido")
                }
        } ?: run {
            idAutor.text = "Usuario no autenticado"
        }
    }

    private fun locUser() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                idLatitud.text = location?.latitude.toString()
                idLongitud.text = location?.longitude.toString()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun selectIMG() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 101) {
            data?.data?.let { uri ->
                uploadImageToFirebase(uri)
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        subidaEnProgreso = true
        Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show()

        val storageReference = FirebaseStorage.getInstance().reference
        val fileReference = storageReference.child("images/${System.currentTimeMillis()}.jpg")

        fileReference.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    uploadedImageUrl = uri.toString()
                    subidaEnProgreso = false
                    Log.d("ImageUpload", "Imagen subida con éxito: $uploadedImageUrl")
                    Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                subidaEnProgreso = false
                Log.e("ImageUpload", "Error al subir la imagen", e)
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun RegisterPoint() {
        subidaEnProgreso = true
        btnSubirInformacion.isEnabled = false

        val latitud = idLatitud.text.toString().toDoubleOrNull()
        val longitud = idLongitud.text.toString().toDoubleOrNull()

        if (latitud == null || longitud == null) {
            Toast.makeText(this, "Latitud y longitud inválidas", Toast.LENGTH_SHORT).show()
            subidaEnProgreso = false
            btnSubirInformacion.isEnabled = true
            return
        }

        val tipo = etTipo.text.toString().trim().lowercase()
        val categoria = etCategoria.text.toString().trim().lowercase()

        if (tipo != "permanente" && tipo != "temporal") {
            Toast.makeText(this, "El tipo debe ser 'permanente' o 'temporal'.", Toast.LENGTH_SHORT).show()
            subidaEnProgreso = false
            btnSubirInformacion.isEnabled = true
            return
        }

        if (categoria != "real" && categoria != "ficticia") {
            Toast.makeText(this, "La categoría debe ser 'real' o 'ficticia'.", Toast.LENGTH_SHORT).show()
            subidaEnProgreso = false
            btnSubirInformacion.isEnabled = true
            return
        }

        val infoLeyenda = hashMapOf(
            "nombre" to etNombre.text.toString(),
            "autor" to idAutor.text.toString(),
            "latitud" to latitud,
            "Longitud" to longitud,
            "Tipo" to tipo,
            "categoria" to categoria,
            "informacion_resumida" to etInfoResumen.text.toString(),
            "informacion_completa" to etInfoCompleta.text.toString(),
            "imagen_url" to uploadedImageUrl
        )

        db.collection("Leyendas").add(infoLeyenda)
            .addOnSuccessListener {
                Log.d("RegisterPoint", "Información subida correctamente")
                Toast.makeText(this, "Información subida correctamente.", Toast.LENGTH_SHORT).show()
                goToHome()
            }
            .addOnFailureListener { e ->
                Log.e("RegisterPoint", "Error al subir la información", e)
                Toast.makeText(this, "Error al subir la información", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                btnSubirInformacion.isEnabled = true
                subidaEnProgreso = false
            }
    }

    private fun goToHome() {
        startActivity(Intent(this, Home::class.java))
        finish()
    }
}

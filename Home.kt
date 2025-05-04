package com.ryujinstudio.story

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.maps.android.SphericalUtil
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class Home : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var idBtnCenter: ImageButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firestoreListener: ListenerRegistration? = null
    private val marcadoresActivos = mutableMapOf<String, Marker>()
    private lateinit var idBtnLogOut: ImageButton
    private lateinit var idClock: TextView
    private lateinit var idBtnPunt: ImageButton
    private lateinit var idBtnInfoApp: ImageButton
    private lateinit var idBtnPet: ImageButton
    private val handler = Handler()
    private val updateClockRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        idBtnCenter = findViewById(R.id.idBtnCenter)
        idBtnLogOut = findViewById(R.id.idBtnLogOut)
        idClock = findViewById(R.id.idClock)
        handler.post(updateClockRunnable)
        idBtnInfoApp = findViewById(R.id.idBtnInfoApp)
        idBtnPet = findViewById(R.id.idBtnPet)
        idBtnPunt = findViewById(R.id.idBtnPunt)

        idBtnCenter.setOnClickListener { locUser() }

        idBtnPet.setOnClickListener {
            startActivity(Intent(this, Peticiones::class.java))
            finish()
        }

        idBtnLogOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        idBtnInfoApp.setOnClickListener {
            val infoAppLayout = layoutInflater.inflate(R.layout.info_app, null)
            val container = findViewById<FrameLayout>(R.id.container_info_app)

            // Verifica si la tarjeta está visible
            if (container.visibility == View.VISIBLE) {
                container.visibility = View.GONE // Oculta la tarjeta si está visible
            } else {
                // Muestra la tarjeta con la información del usuario
                InfoUser(infoAppLayout)
                container.removeAllViews() // Limpiar el contenedor si tiene vistas
                container.addView(infoAppLayout) // Agregar el layout inflado al contenedor
                container.visibility = View.VISIBLE // Mostrar la tarjeta
            }
        }


        val TAG = "PuntoCercano"
        var isInfoVisible = false
        var isProcessing = false

        idBtnPunt.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            isProcessing = true

            val frame = findViewById<FrameLayout>(R.id.container_info_point)

            if (isInfoVisible) {
                frame.removeAllViews()
                frame.visibility = View.GONE
                isInfoVisible = false
                isProcessing = false
                return@setOnClickListener
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                isProcessing = false
                Log.w(TAG, "Permiso de ubicación no concedido")
                return@setOnClickListener
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "La ubicación es nula")
                    isProcessing = false
                    return@addOnSuccessListener
                }

                val userLocation = LatLng(location.latitude, location.longitude)
                Log.d(TAG, "Ubicación del usuario: ${userLocation.latitude}, ${userLocation.longitude}")

                db.collection("Leyendas").get()
                    .addOnSuccessListener { result ->
                        var nearestDoc: DocumentSnapshot? = null
                        var shortestDistance = Double.MAX_VALUE

                        for (document in result) {
                            val lat = document.getDouble("latitud")
                            val lng = document.getDouble("Longitud")

                            if (lat != null && lng != null) {
                                val pointLocation = LatLng(lat, lng)
                                val distance = SphericalUtil.computeDistanceBetween(userLocation, pointLocation)

                                Log.d(TAG, "Distancia a ${document.getString("nombre")}: $distance metros")

                                if (distance < shortestDistance) {
                                    shortestDistance = distance
                                    nearestDoc = document
                                }
                            } else {
                                Log.w(TAG, "Documento con coordenadas inválidas: ${document.id}")
                            }
                        }

                        if (nearestDoc != null) {
                            val nombre = nearestDoc.getString("nombre") ?: "Sin nombre"
                            val autor = nearestDoc.getString("autor") ?: "Desconocido"
                            val tipo = nearestDoc.getString("Tipo") ?: "-"
                            val categoria = nearestDoc.getString("categoria") ?: "-"
                            val info = nearestDoc.getString("informacion_completa") ?: "Sin información"
                            val imagenUrl = nearestDoc.getString("imagen_url") ?: ""

                            Log.d(TAG, "Punto más cercano: $nombre ($shortestDistance m)")

                            frame.removeAllViews()
                            val cardView = layoutInflater.inflate(R.layout.point_details, frame, false)

                            val txtNombre = cardView.findViewById<TextView>(R.id.title)
                            val txtAutor = cardView.findViewById<TextView>(R.id.autor)
                            val txtTipo = cardView.findViewById<TextView>(R.id.tipo)
                            val txtCategoria = cardView.findViewById<TextView>(R.id.categoria)
                            val txtInfo = cardView.findViewById<TextView>(R.id.informacion_completa)
                            val imgView = cardView.findViewById<ImageView>(R.id.imagen)

                            txtNombre.text = nombre
                            txtAutor.text = "Autor: $autor"
                            txtTipo.text = tipo
                            txtCategoria.text = categoria
                            txtInfo.text = info

                            Glide.with(this)
                                .load(imagenUrl)
                                .placeholder(R.drawable.loading_placeholder)
                                .error(R.drawable.image_error)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                .into(imgView)

                            frame.addView(cardView)
                            frame.visibility = View.VISIBLE
                            isInfoVisible = true
                        } else {
                            Toast.makeText(this, "No se encontraron puntos cercanos", Toast.LENGTH_LONG).show()
                            Log.i(TAG, "No se encontró ningún punto cercano")
                        }

                        isProcessing = false
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al obtener puntos", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error al obtener puntos de Firestore", e)
                        isProcessing = false
                    }

            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener la ubicación", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error al obtener la ubicación", e)
                isProcessing = false
            }
        }




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun InfoUser(view: View) {
        val user = FirebaseAuth.getInstance().currentUser

        // Obtener las vistas del layout inflado
        val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
        val tvCorreo = view.findViewById<TextView>(R.id.tvCorreo)
        val tvUID = view.findViewById<TextView>(R.id.tvUID)
        val tvFecha = view.findViewById<TextView>(R.id.tvFecha)

        // Verificar si el usuario está autenticado
        if (user != null) {
            val email = user.email ?: ""
            val uid = user.uid

            tvCorreo.text = "Correo: $email"
            tvUID.text = "UID: $uid"

            // Consultar Firestore para obtener más información
            db.collection("Usuarios")
                .whereEqualTo("correo", email)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val doc = result.documents[0]
                        val nombre = doc.getString("nombre") ?: "Desconocido"
                        val fechaRegistro = doc.get("fechaRegistro") // Puede ser un Timestamp o Date

                        // Verificar si 'fechaRegistro' es un Timestamp o Date
                        val fecha = when (fechaRegistro) {
                            is Timestamp -> fechaRegistro.toDate()?.let { formatDate(it) } ?: "Sin registrar"
                            is Date -> formatDate(fechaRegistro) // Si es Date, formatearlo
                            else -> "Sin registrar" // En caso de que no sea de tipo Date o Timestamp
                        }

                        tvUsername.text = "Nombre: $nombre"
                        tvFecha.text = "Fecha de registro: $fecha"
                    } else {
                        tvUsername.text = "Nombre: No encontrado"
                        tvFecha.text = "Fecha de registro: -"
                    }
                }
                .addOnFailureListener {
                    Log.e("infouser", "Error obteniendo datos: ${it.message}")
                    tvUsername.text = "Error obteniendo datos"
                    tvFecha.text = ""
                }

        } else {
            // Si el usuario no está autenticado
            tvUsername.text = "Usuario no autenticado"
            tvCorreo.text = ""
            tvUID.text = ""
            tvFecha.text = ""
        }
    }

    // Función para formatear la fecha en String
    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return format.format(date)
    }

    private fun updateTime() {
        val fechaActual = Timestamp.now()
        val sdf = SimpleDateFormat("hh:mm", Locale.getDefault())
        val formattedTime = sdf.format(fechaActual.toDate())
        idClock.text = formattedTime
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = false
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
            isZoomGesturesEnabled = true
        }

        val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

        if (!success) {
            Log.e("MapStyle", "No se pudo aplicar el estilo al mapa.")
        }

        map.setMinZoomPreference(16f)
        map.setMaxZoomPreference(19f)

        googleMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? = null

            override fun getInfoContents(marker: Marker): View {
                val view = layoutInflater.inflate(R.layout.custom_info_window, null)
                val datos = marker.tag as? Map<String, String>
                view.findViewById<TextView>(R.id.title).text = marker.title
                view.findViewById<TextView>(R.id.snippet).text = marker.snippet
                view.findViewById<TextView>(R.id.autor).text = datos?.get("autor") ?: "Autor desconocido"
                view.findViewById<TextView>(R.id.tipo).text = datos?.get("tipo") ?: "Desconocida"
                return view
            }
        })

        locUser()
        escucharFirestore()
    }

    private fun escucharFirestore() {
        firestoreListener?.remove()  // Primero eliminamos el listener anterior
        marcadoresActivos.clear()  // Limpiamos los marcadores activos para evitar duplicados

        firestoreListener = db.collection("Leyendas")  // Accedemos a la colección "Leyendas"
            .addSnapshotListener { snapshot, error ->  // Añadimos el listener
                if (error != null) {
                    Log.e("Firestore", "Error al escuchar cambios en Firestore", error)
                    Toast.makeText(this, "Error conectando con Firestore: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                snapshot?.let { actualizarMarcadores(it) }
            }
    }

    private fun actualizarMarcadores(snapshot: QuerySnapshot) {
        val documentosActuales = snapshot.documents.map { it.id }.toSet()

        // Eliminar marcadores que ya no existen en Firestore
        marcadoresActivos.keys.filterNot { documentosActuales.contains(it) }.forEach { id ->
            marcadoresActivos[id]?.remove()
            marcadoresActivos.remove(id)
        }

        // Recorrer los documentos para agregar o actualizar marcadores
        for (doc in snapshot.documents) {
            val id = doc.id
            val nombre = doc.getString("nombre") ?: "Sin nombre"
            val resumen = doc.getString("informacion_resumida") ?: ""
            val autor = doc.getString("autor") ?: "Autor desconocido"

            // Verificar si los valores de latitud y longitud son numéricos
            val lat = doc.get("latitud") as? Double ?: continue  // Si no es Double, se omite el documento
            val lon = doc.get("Longitud") as? Double ?: continue  // Lo mismo para longitud

            val tipo = doc.getString("Tipo")?.lowercase() ?: "default"
            val posicion = LatLng(lat, lon)

            val color = when (tipo) {
                "permanente" -> BitmapDescriptorFactory.HUE_GREEN
                "temporal" -> BitmapDescriptorFactory.HUE_BLUE
                else -> BitmapDescriptorFactory.HUE_ORANGE
            }

            if (marcadoresActivos.containsKey(id)) {
                // Si el marcador ya existe, actualizamos su posición y otros atributos
                val marcador = marcadoresActivos[id]
                marcador?.apply {
                    position = posicion
                    title = nombre
                    snippet = resumen
                    tag = mapOf("autor" to autor, "tipo" to tipo)
                }
            } else {
                // Si el marcador no existe, lo agregamos a Google Maps
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(nombre)
                        .snippet(resumen)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                )
                marker?.tag = mapOf("autor" to autor, "tipo" to tipo)
                marker?.let { marcadoresActivos[id] = it }
            }
        }
    }

    private fun locUser() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val pos = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 18f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        firestoreListener?.remove()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        handler.removeCallbacks(updateClockRunnable)
        firestoreListener?.remove()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}


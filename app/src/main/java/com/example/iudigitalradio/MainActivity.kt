package com.example.iudigitalradio

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// Modelo de datos para las emisoras locales
data class Emisora(
    val id: Int,
    val nombre: String,
    val genero: String,
    val dial: String
)


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IUDigitalRadioScreen()
                }
            }
        }
    }
}


@Composable
fun IUDigitalRadioScreen() {

    val context = LocalContext.current

    // Catálogo de emisoras locales
    val listaEmisoras = remember {
        listOf(
            Emisora(1, "IU Digital Stereo", "Educativa / Institucional", "101.5 FM"),
            Emisora(2, "Radio Rock & Pop", "Rock Clásico", "98.3 FM"),
            Emisora(3, "Salsa & Sabor", "Salsa / Tropical", "105.9 FM"),
            Emisora(4, "Urbana Beats", "Reggaeton / Urban", "92.1 FM"),
            Emisora(5, "Noticias IUD", "Informativa", "89.0 AM")
        )
    }


    // =========================================================================
    // GESTIÓN DE ESTADO PERSISTENTE
    // =========================================================================

    var isPlaying by rememberSaveable {
        mutableStateOf(false)
    }

    var isMuted by rememberSaveable {
        mutableStateOf(false)
    }

    // Almacenamos únicamente el ID entero con rememberSaveable para mayor estabilidad
    var emisoraSeleccionadaId by rememberSaveable {
        mutableIntStateOf(listaEmisoras[0].id)
    }

    // Obtenemos el objeto Emisora derivado del ID activo
    val emisoraSeleccionada =
        listaEmisoras.first { it.id == emisoraSeleccionadaId }

    // Estado para la foto de perfil en memoria
    var fotoPerfil by remember {
        mutableStateOf<Bitmap?>(null)
    }

    // URI utilizada para recibir la fotografía de la cámara
    var fotoUri by remember {
        mutableStateOf<Uri?>(null)
    }


    // =========================================================================
    // FUNCIÓN DE RETROALIMENTACIÓN HÁPTICA / VIBRACIÓN
    // =========================================================================

    fun ejecutarVibracion() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as VibratorManager

            val vibrator = vibratorManager.defaultVibrator

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    80,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            @Suppress("DEPRECATION")
            val vibrator =
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }


    // =========================================================================
    // LANZADORES DE CÁMARA Y PERMISOS DE SISTEMA
    // =========================================================================

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { fotografiaGuardada ->

        if (fotografiaGuardada && fotoUri != null) {

            try {

                context.contentResolver.openInputStream(fotoUri!!).use {
                    fotoPerfil = BitmapFactory.decodeStream(it)
                }

            } catch (e: Exception) {

                Toast.makeText(
                    context,
                    "No se pudo cargar la fotografía",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {

            Toast.makeText(
                context,
                "No se recibió la fotografía",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->

        if (isGranted) {

            val values = android.content.ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "foto_perfil_iudigital.jpg"
                )
                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
                )
            }

            fotoUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            if (fotoUri != null) {
                cameraLauncher.launch(fotoUri!!)
            } else {
                Toast.makeText(
                    context,
                    "No se pudo preparar la cámara",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {

            Toast.makeText(
                context,
                "Permiso de cámara denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        // --- SECCIÓN 1: PERFIL DE USUARIO ---

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (fotoPerfil != null) {

                    Image(
                        bitmap = fotoPerfil!!.asImageBitmap(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(65.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .size(65.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            "Sin Foto",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.width(14.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        "Usuario IUDigital",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        "Oyente Activo",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }


                Button(onClick = {

                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )

                }) {

                    Text("Foto")
                }
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // --- SECCIÓN 2: REPRODUCTOR CENTRAL ---

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "EMISORA EN VIVO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )


                Spacer(
                    modifier = Modifier.height(6.dp)
                )


                Text(
                    emisoraSeleccionada.nombre,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )


                Text(
                    "${emisoraSeleccionada.dial} - ${emisoraSeleccionada.genero}",
                    fontSize = 14.sp
                )


                Spacer(
                    modifier = Modifier.height(16.dp)
                )


                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Button(onClick = {

                        ejecutarVibracion()

                        isPlaying = !isPlaying

                    }) {

                        Text(
                            if (isPlaying) "Pausar"
                            else "Reproducir"
                        )
                    }


                    OutlinedButton(onClick = {

                        ejecutarVibracion()

                        isMuted = !isMuted

                    }) {

                        Text(
                            if (isMuted) "Unmute"
                            else "Mute"
                        )
                    }
                }
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // --- SECCIÓN 3: CATÁLOGO DE EMISORAS ---

        Text(
            "Lista de Emisoras",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )


        // SELECCIÓN EN TIEMPO REAL DESDE LA LISTA VERTICAL

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(listaEmisoras) { emisora ->

                val esSeleccionada =
                    emisora.id == emisoraSeleccionadaId


                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                            ejecutarVibracion()

                            emisoraSeleccionadaId =
                                emisora.id

                            isPlaying = true
                        },

                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (esSeleccionada)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                emisora.nombre,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                emisora.genero,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }


                        Text(
                            emisora.dial,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun IUDigitalRadioPreview() {

    MaterialTheme {

        IUDigitalRadioScreen()

    }
}
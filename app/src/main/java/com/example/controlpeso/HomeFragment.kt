package com.example.controlpeso

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.databinding.FragmentHomeBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val apiKey = "AIzaSyA5NV-6UE-kXTh8hHJ3UHRpDWDyf20917Y"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupListeners()
        loadDataFromFirestore()
    }

    private fun setupTabs() {
        binding.homeTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {} 
                    1 -> irAResultado("Dieta")
                    2 -> irAResultado("Rutina")
                    3 -> buscarEspecialistas()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab?.position == 3) buscarEspecialistas()
            }
        })
    }

    private fun loadDataFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombres = doc.getString("nombres") ?: ""
                    val pesoActual = (doc.getDouble("pesoActual") ?: 0.0).toFloat()
                    val talla = (doc.getLong("altura") ?: 0).toInt()
                    val edad = (doc.getLong("edad") ?: 0).toInt()
                    val genero = doc.getString("genero") ?: ""
                    
                    binding.tvHomeUserHeader.text = "Hola, $nombres"
                    binding.tvProfNombreDisplay.text = nombres
                    binding.tvProfEdad.text = "Edad: $edad años"
                    binding.tvProfGenero.text = "Género: $genero"
                    binding.tvProfAltura.text = "Altura: $talla cm"
                    binding.tvProfPeso.text = "Peso: $pesoActual kg"

                    calcularIMCyCalorias(pesoActual, talla, edad)
                    consultarRecomendacionesIA(pesoActual, talla, genero)
                }
            }

        db.collection("usuarios").document(uid)
            .collection("historial_peso")
            .orderBy("fecha", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val entries = mutableListOf<Entry>()
                val dates = mutableListOf<String>()
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                querySnapshot.documents.forEachIndexed { index, doc ->
                    val peso = (doc.getDouble("valor") ?: 0.0).toFloat()
                    val fecha = doc.getTimestamp("fecha")?.toDate() ?: Date()
                    entries.add(Entry(index.toFloat(), peso))
                    dates.add(sdf.format(fecha))
                }
                setupChart(entries, dates)
            }
    }

    private fun calcularIMCyCalorias(peso: Float, talla: Int, edad: Int) {
        val tallaM = talla / 100f
        if (tallaM > 0) {
            val imc = peso / (tallaM * tallaM)
            binding.tvDashImc.text = String.format(Locale.getDefault(), "%.1f", imc)
            val (categoria, color) = when {
                imc < 18.5 -> "Bajo Peso" to R.color.imc_bajo
                imc < 25.0 -> "Normal" to R.color.imc_normal
                imc < 30.0 -> "Sobrepeso" to R.color.imc_sobrepeso
                else -> "Obesidad" to R.color.imc_obesidad
            }
            binding.tvDashImcLabel.text = categoria
            binding.tvDashImcLabel.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
            val tmb = (10 * peso) + (6.25 * talla) - (5 * edad) + 5
            binding.tvDashCalorias.text = String.format(Locale.getDefault(), "%.0f", tmb)
            binding.tvMantenimientoLabel.text = "Mantenimiento: ${String.format(Locale.getDefault(), "%.0f", tmb + 300)} kcal"
        }
    }

    private fun setupChart(entries: List<Entry>, dates: List<String>) {
        if (entries.isEmpty()) return
        val dataSet = LineDataSet(entries, "Peso (kg)").apply {
            color = Color.BLACK
            setCircleColor(Color.BLACK)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.LTGRAY
        }
        binding.weightChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dates.getOrNull(value.toInt()) ?: ""
                }
            }
            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(true)
            animateX(1000)
            invalidate()
        }
    }

    private fun consultarRecomendacionesIA(peso: Float, talla: Int, genero: String) {
        val generativeModel = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey)
        val prompt = "Usuario: $genero, $peso kg, $talla cm. Dame 4 consejos de 15 palabras: Salud, Nutricion, Ejercicio, Seguimiento."
        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val lineas = (response.text ?: "").split("\n")
                lineas.forEach { linea ->
                    when {
                        linea.contains("Salud", true) -> binding.tvIAEstadoSalud.text = linea
                        linea.contains("Nutricion", true) || linea.contains("Nutrición", true) -> binding.tvIANutricion.text = linea
                        linea.contains("Ejercicio", true) -> binding.tvIAEjercicio.text = linea
                        linea.contains("Seguimiento", true) -> binding.tvIASeguimiento.text = linea
                    }
                }
            } catch (e: Exception) {
                binding.tvIAEstadoSalud.text = "Consejos no disponibles"
            }
        }
    }

    private fun setupListeners() {
        binding.btnCerrarSesion.setOnClickListener {
            // 1. Cerrar sesión en Firebase
            auth.signOut()
            
            // 2. Marcar bandera local como cerrada
            val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isLoggedIn", false).apply()
            
            // 3. Navegar al Login limpiando toda la pila de pantallas
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }
        binding.btnVerPerfil.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_editProfileFragment)
        }
    }

    private fun irAResultado(tipo: String) {
        val bundle = Bundle().apply { putString("tipo_plan", tipo) }
        findNavController().navigate(R.id.action_homeFragment_to_planResultFragment, bundle)
    }

    private fun buscarEspecialistas() {
        val gmmIntentUri = Uri.parse("geo:0,0?q=nutricionistas")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

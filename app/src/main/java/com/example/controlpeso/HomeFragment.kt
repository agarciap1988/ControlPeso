package com.example.controlpeso

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.databinding.FragmentHomeBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val apiKey = "AIzaSyAflVqGq9EUZYbkczrbv229jM1UPgVkQEc"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupResumenData()
        setupTabs()
        setupListeners()
        consultarRecomendacionesIA()
    }

    private fun setupTabs() {
        binding.homeTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> mostrarResumen()
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

    private fun setupResumenData() {
        binding.tvHomeUserHeader.text = "Hola, ${UserSession.nombres}"
        
        val peso = UserSession.pesoActual
        val tallaM = UserSession.talla / 100f
        if (tallaM > 0) {
            val imc = peso / (tallaM * tallaM)
            binding.tvDashImc.text = String.format(Locale.getDefault(), "%.1f", imc)
            
            val (categoria, color) = when {
                imc < 18.5 -> "Bajo Peso" to R.color.imc_bajo
                imc < 25.0 -> "Normal" to R.color.imc_normal
                imc < 30.0 -> "Sobrepeso" to R.color.imc_sobrepeso
                imc < 35.0 -> "Obesidad Grado I" to R.color.imc_obesidad
                else -> "Obesidad Crítica" to R.color.imc_obesidad_critica
            }
            binding.tvDashImcLabel.text = categoria
            binding.tvDashImcLabel.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
        }

        binding.tvProfEdad.text = "Edad: ${UserSession.edad} años"
        binding.tvProfGenero.text = "Género: ${UserSession.genero}"
        binding.tvProfAltura.text = "Altura: ${UserSession.talla} cm"
        binding.tvProfPeso.text = "Peso: ${UserSession.pesoActual} kg"
        
        val calorias = (10 * UserSession.pesoActual) + (6.25 * UserSession.talla) - (5 * UserSession.edad) + 5
        binding.tvDashCalorias.text = String.format(Locale.getDefault(), "%.0f", calorias)
        binding.tvMantenimientoLabel.text = "Mantenimiento: ${String.format(Locale.getDefault(), "%.0f", calorias + 500)} kcal"
    }

    private fun consultarRecomendacionesIA() {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val prompt = """
            Eres un experto en salud. Analiza este perfil brevemente:
            - Nombre: ${UserSession.nombres}
            - IMC: ${binding.tvDashImc.text}
            - Enfermedad: ${UserSession.enfermedadPreexistente}
            - Objetivo: ${UserSession.objetivo}
            
            Dame 4 consejos ultra breves (máximo 15 palabras cada uno) para:
            1. Estado de Salud
            2. Nutrición
            3. Ejercicio
            4. Plan de Seguimiento
            
            Formato: 
            Salud: [consejo]
            Nutrición: [consejo]
            Ejercicio: [consejo]
            Seguimiento: [consejo]
        """.trimIndent()

        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: ""
                
                // Parsear respuesta simple
                val lineas = text.split("\n")
                lineas.forEach { linea ->
                    when {
                        linea.contains("Salud", true) -> binding.tvIAEstadoSalud.text = linea
                        linea.contains("Nutrición", true) -> binding.tvIANutricion.text = linea
                        linea.contains("Ejercicio", true) -> binding.tvIAEjercicio.text = linea
                        linea.contains("Seguimiento", true) -> binding.tvIASeguimiento.text = linea
                    }
                }
            } catch (e: Exception) {
                binding.tvIAEstadoSalud.text = "Error al cargar recomendaciones."
            }
        }
    }

    private fun mostrarResumen() {
        binding.layoutResumen.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        binding.btnCerrarSesion.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isLoggedIn", false).apply()
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }

        binding.btnVerPerfil.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_editProfileFragment)
        }
    }

    private fun irAResultado(tipo: String) {
        val bundle = Bundle().apply {
            putString("tipo_plan", tipo)
        }
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

package com.example.controlpeso

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.database.AppDatabase
import com.example.controlpeso.database.UserEntity
import com.example.controlpeso.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()

        binding.btnContinuarPerfil.setOnClickListener {
            if (validarPerfil()) {
                saveProfileData()
            }
        }
    }

    private fun setupDropdowns() {
        val generos = arrayOf("Masculino", "Femenino", "Otro")
        val adapterGenero = ArrayAdapter(requireContext(), R.layout.list_item, generos)
        binding.spinnerGenero.setAdapter(adapterGenero)

        val enfermedades = arrayOf("Ninguna", "Diabetes", "Colesterol elevado", "Hipertension")
        val adapterEnfermedad = ArrayAdapter(requireContext(), R.layout.list_item, enfermedades)
        binding.spinnerEnfermedad.setAdapter(adapterEnfermedad)

        val objetivos = arrayOf("Perder Peso", "Mantener Peso", "Ganar Peso")
        val adapterObjetivo = ArrayAdapter(requireContext(), R.layout.list_item, objetivos)
        binding.spinnerObjetivo.setAdapter(adapterObjetivo)

        val actividades = arrayOf(
            "Sedentario (poco o ningún ejercicio)",
            "Ligero (ejercicio 1-3 días/semana)",
            "Moderado (ejercicio 3-5 días/semana)",
            "Activo (ejercicio 6-7 días/semana)",
            "Muy Activo (ejercicio intenso diario)"
        )
        val adapterActividad = ArrayAdapter(requireContext(), R.layout.list_item, actividades)
        binding.spinnerActividad.setAdapter(adapterActividad)
    }

    private fun validarPerfil(): Boolean {
        if (binding.etProfNombre.text.isNullOrBlank() || 
            binding.etProfEdad.text.isNullOrBlank() ||
            binding.etProfAltura.text.isNullOrBlank() ||
            binding.etProfPeso.text.isNullOrBlank()) {
            Toast.makeText(context, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveProfileData() {
        val nombre = binding.etProfNombre.text.toString()
        val edad = binding.etProfEdad.text.toString().toIntOrNull() ?: 0
        val altura = binding.etProfAltura.text.toString().toIntOrNull() ?: 0
        val peso = binding.etProfPeso.text.toString().toFloatOrNull() ?: 0f
        val genero = binding.spinnerGenero.text.toString()
        val enfermedad = binding.spinnerEnfermedad.text.toString()
        val objetivo = binding.spinnerObjetivo.text.toString()
        val actividad = binding.spinnerActividad.text.toString()

        val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("reg_email", "usuario@ejemplo.com") ?: "usuario@ejemplo.com"

        // 1. Guardar en SharedPreferences (Persistencia Rápida)
        val editor = prefs.edit()
        editor.putString("nombres", nombre)
        editor.putInt("edad", edad)
        editor.putInt("talla", altura)
        editor.putFloat("peso", peso)
        editor.putFloat("peso_inicial", peso)
        editor.putString("genero", genero)
        editor.putString("enfermedad", enfermedad)
        editor.putString("objetivo", objetivo)
        editor.putString("actividad", actividad)
        editor.putBoolean("isRegistered", true)
        editor.putBoolean("isLoggedIn", true)
        editor.apply()

        // 2. Actualizar Sesión en memoria
        UserSession.nombres = nombre
        UserSession.edad = edad
        UserSession.talla = altura
        UserSession.pesoInicial = peso
        UserSession.pesoActual = peso
        UserSession.genero = genero
        UserSession.enfermedadPreexistente = enfermedad
        UserSession.objetivo = objetivo
        UserSession.nivelActividad = actividad

        // 3. Guardar en Room Database
        val userEntity = UserEntity(
            email = email,
            nombres = nombre,
            genero = genero,
            edad = edad,
            altura = altura,
            pesoInicial = peso,
            pesoActual = peso,
            enfermedad = enfermedad,
            objetivo = objetivo,
            actividad = actividad
        )

        val db = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            db.userDao().insertUser(userEntity)
            Toast.makeText(context, "¡Perfil guardado correctamente!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

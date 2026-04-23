package com.example.controlpeso

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        binding.btnContinuarPerfil.setOnClickListener {
            if (validarPerfil()) {
                guardarPerfilEnFirestore()
            }
        }
    }

    private fun setupDropdowns() {
        val generos = arrayOf("Masculino", "Femenino", "Otro")
        binding.spinnerGenero.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, generos))

        val enfermedades = arrayOf("Ninguna", "Diabetes", "Colesterol elevado", "Hipertension")
        binding.spinnerEnfermedad.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, enfermedades))

        val objetivos = arrayOf("Perder Peso", "Mantener Peso", "Ganar Peso")
        binding.spinnerObjetivo.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, objetivos))

        val actividades = arrayOf(
            "Sedentario (poco o ningún ejercicio)",
            "Ligero (ejercicio 1-3 días/semana)",
            "Moderado (ejercicio 3-5 días/semana)",
            "Activo (ejercicio 6-7 días/semana)",
            "Muy Activo (ejercicio intenso diario)"
        )
        binding.spinnerActividad.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, actividades))
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

    private fun guardarPerfilEnFirestore() {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        
        val nombre = binding.etProfNombre.text.toString()
        val peso = binding.etProfPeso.text.toString().toFloatOrNull() ?: 0f

        // Objeto de datos para Firebase
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "nombres" to nombre,
            "edad" to (binding.etProfEdad.text.toString().toIntOrNull() ?: 0),
            "genero" to binding.spinnerGenero.text.toString(),
            "altura" to (binding.etProfAltura.text.toString().toIntOrNull() ?: 0),
            "pesoInicial" to peso,
            "pesoActual" to peso,
            "enfermedad" to binding.spinnerEnfermedad.text.toString(),
            "objetivo" to binding.spinnerObjetivo.text.toString(),
            "actividad" to binding.spinnerActividad.text.toString()
        )

        binding.btnContinuarPerfil.isEnabled = false
        binding.btnContinuarPerfil.text = "Sincronizando con la nube..."

        // Aquí es donde sucede la magia: Se crea la colección "usuarios" y el documento con tu UID
        db.collection("usuarios").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                // También actualizamos la sesión local para el Dashboard
                UserSession.nombres = nombre
                UserSession.pesoActual = peso
                UserSession.talla = userMap["altura"] as Int
                
                Toast.makeText(context, "¡Perfil sincronizado en la nube!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
            }
            .addOnFailureListener { e ->
                binding.btnContinuarPerfil.isEnabled = true
                binding.btnContinuarPerfil.text = "Continuar"
                Toast.makeText(context, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

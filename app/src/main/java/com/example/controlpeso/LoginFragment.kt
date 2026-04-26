package com.example.controlpeso

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)

        //  Aplicar modo guardado al iniciar
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        //  Botón modo oscuro (asegúrate que exista en tu XML)
        binding.btnDarkMode.setOnClickListener {
            val current = prefs.getBoolean("dark_mode", false)

            if (current) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            prefs.edit().putBoolean("dark_mode", !current).apply()
        }

        // Verificamos si ya hay un usuario de Firebase logueado
        if (auth.currentUser != null) {
            checkAndLoadData(auth.currentUser!!.uid)
        }

        binding.btnIniciarSesion.setOnClickListener {
            validarLoginFirebase()
        }

        binding.btnToggleRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun validarLoginFirebase() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnIniciarSesion.isEnabled = false
        binding.btnIniciarSesion.text = "Iniciando sesión..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""
                    checkAndLoadData(uid)
                } else {
                    binding.btnIniciarSesion.isEnabled = true
                    binding.btnIniciarSesion.text = "Iniciar Sesión"
                    Toast.makeText(context, "Error: Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkAndLoadData(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    UserSession.email = document.getString("email") ?: ""
                    UserSession.nombres = document.getString("nombres") ?: ""
                    UserSession.genero = document.getString("genero") ?: ""
                    UserSession.edad = (document.getLong("edad") ?: 0).toInt()
                    UserSession.talla = (document.getLong("altura") ?: 0).toInt()
                    UserSession.pesoInicial = (document.getDouble("pesoInicial") ?: 0.0).toFloat()
                    UserSession.pesoActual = (document.getDouble("pesoActual") ?: 0.0).toFloat()
                    UserSession.enfermedadPreexistente = document.getString("enfermedad") ?: "Ninguna"
                    UserSession.objetivo = document.getString("objetivo") ?: ""
                    UserSession.nivelActividad = document.getString("actividad") ?: ""

                    val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("isLoggedIn", true).apply()

                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    findNavController().navigate(R.id.action_loginFragment_to_profileFragment)
                }
            }
            .addOnFailureListener {
                binding.btnIniciarSesion.isEnabled = true
                binding.btnIniciarSesion.text = "Iniciar Sesión"
                Toast.makeText(context, "Error al conectar con la nube", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

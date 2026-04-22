package com.example.controlpeso

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificamos si hay una sesión activa para saltar el login
        checkAutoLogin()

        binding.btnIniciarSesion.setOnClickListener {
            validarLogin()
        }

        binding.btnToggleRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun checkAutoLogin() {
        val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            loadUserSession(prefs)
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    private fun validarLogin() {
        val emailIngresado = binding.etEmail.text.toString().trim()
        val passwordIngresada = binding.etPassword.text.toString().trim()

        val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
        val emailRegistrado = prefs.getString("reg_email", "")
        val passwordRegistrada = prefs.getString("reg_password", "")

        if (emailIngresado.isNotEmpty() && emailIngresado == emailRegistrado && passwordIngresada == passwordRegistrada) {
            // Guardamos que la sesión está activa
            prefs.edit().putBoolean("isLoggedIn", true).apply()

            loadUserSession(prefs)
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        } else {
            Toast.makeText(context, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserSession(prefs: android.content.SharedPreferences) {
        UserSession.nombres = prefs.getString("nombres", "") ?: ""
        UserSession.pesoActual = prefs.getFloat("peso", 0f)
        UserSession.pesoInicial = prefs.getFloat("peso_inicial", UserSession.pesoActual)
        UserSession.talla = prefs.getInt("talla", 0)
        UserSession.edad = prefs.getInt("edad", 0)
        UserSession.genero = prefs.getString("genero", "") ?: ""
        UserSession.enfermedadPreexistente = prefs.getString("enfermedad", "Ninguna") ?: "Ninguna"
        UserSession.objetivo = prefs.getString("objetivo", "") ?: ""
        UserSession.nivelActividad = prefs.getString("actividad", "") ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

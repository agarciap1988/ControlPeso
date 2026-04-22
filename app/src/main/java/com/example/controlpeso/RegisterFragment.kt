package com.example.controlpeso

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnContinuarRegistro.setOnClickListener {
            if (validarCredenciales()) {
                saveBasicCredentials()
                findNavController().navigate(R.id.action_registerFragment_to_profileFragment)
            }
        }

        binding.btnTabLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun validarCredenciales(): Boolean {
        val email = binding.etRegEmail.text.toString().trim()
        val pass = binding.etRegPassword.text.toString().trim()
        val confirm = binding.etRegConfirmPassword.text.toString().trim()

        if (email.isBlank() || pass.isBlank()) {
            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }

        // VALIDACIÓN DE USUARIO ÚNICO
        val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
        val emailRegistrado = prefs.getString("reg_email", "")
        if (email == emailRegistrado) {
            Toast.makeText(context, "Este correo ya se encuentra registrado", Toast.LENGTH_LONG).show()
            return false
        }

        if (pass != confirm) {
            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveBasicCredentials() {
        val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("reg_email", binding.etRegEmail.text.toString().trim())
        editor.putString("reg_password", binding.etRegPassword.text.toString().trim())
        editor.apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

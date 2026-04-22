package com.example.controlpeso

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.database.AppDatabase
import com.example.controlpeso.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentData()

        binding.btnGuardarPeso.setOnClickListener {
            actualizarPeso()
        }

        binding.btnActualizarPass.setOnClickListener {
            actualizarContrasena()
        }

        binding.btnCerrarEdit.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadCurrentData() {
        binding.etEditNombre.setText(UserSession.nombres)
        binding.etEditAltura.setText(UserSession.talla.toString())
        binding.etEditPeso.setText(UserSession.pesoActual.toString())
        
        binding.etEditNombre.isEnabled = false
        binding.etEditAltura.isEnabled = false
    }

    private fun actualizarPeso() {
        val nuevoPesoStr = binding.etEditPeso.text.toString()
        val nuevoPeso = nuevoPesoStr.toFloatOrNull()

        if (nuevoPeso != null && nuevoPeso > 0) {
            UserSession.pesoActual = nuevoPeso
            val db = AppDatabase.getDatabase(requireContext())
            lifecycleScope.launch {
                db.userDao().updatePesoActual(nuevoPeso)
                
                val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
                prefs.edit().putFloat("peso", nuevoPeso).apply()

                Toast.makeText(context, "¡Peso actualizado correctamente!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        } else {
            Toast.makeText(context, "Por favor, ingresa un peso válido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarContrasena() {
        val newPass = binding.etEditNewPassword.text.toString().trim()
        if (newPass.isNotEmpty()) {
            if (newPass.length < 6) {
                Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return
            }
            val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("reg_password", newPass).apply()
            
            binding.etEditNewPassword.text?.clear()
            Toast.makeText(context, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Ingresa una nueva contraseña", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

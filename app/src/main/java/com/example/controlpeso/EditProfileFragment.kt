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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val dbFirestore = FirebaseFirestore.getInstance()

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
            binding.btnGuardarPeso.isEnabled = false
            binding.btnGuardarPeso.text = "Sincronizando..."

            // 1. Actualizar Sesión Local
            UserSession.pesoActual = nuevoPeso

            // 2. Actualizar Room (Local)
            val dbLocal = AppDatabase.getDatabase(requireContext())
            lifecycleScope.launch {
                dbLocal.userDao().updatePesoActual(nuevoPeso)
                
                // 3. Actualizar SharedPreferences
                val prefs = requireActivity().getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
                prefs.edit().putFloat("peso", nuevoPeso).apply()

                // 4. ACTUALIZAR EN FIRESTORE (LA NUBE)
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    dbFirestore.collection("usuarios").document(uid)
                        .update("pesoActual", nuevoPeso)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Nube actualizada: ${nuevoPeso}kg", Toast.LENGTH_SHORT).show()
                            // SOLO navegamos hacia atrás cuando la nube confirma el éxito
                            findNavController().navigateUp()
                        }
                        .addOnFailureListener { e ->
                            binding.btnGuardarPeso.isEnabled = true
                            binding.btnGuardarPeso.text = "Guardar"
                            Toast.makeText(context, "Error al sincronizar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        } else {
            Toast.makeText(context, "Por favor, ingresa un peso válido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarContrasena() {
        val newPass = binding.etEditNewPassword.text.toString().trim()
        if (newPass.isNotEmpty() && newPass.length >= 6) {
            auth.currentUser?.updatePassword(newPass)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Contraseña actualizada en Firebase", Toast.LENGTH_SHORT).show()
                        binding.etEditNewPassword.text?.clear()
                    } else {
                        Toast.makeText(context, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(context, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.controlpeso

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.controlpeso.databinding.FragmentPlanResultBinding
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import java.util.Calendar

class PlanResultFragment : Fragment() {
    private var _binding: FragmentPlanResultBinding? = null
    private val binding get() = _binding!!

    private val apiKey = "AIzaSyA5NV-6UE-kXTh8hHJ3UHRpDWDyf20917Y"
    private var tipoPlan: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleAlarm()
        } else {
            Toast.makeText(context, "Permiso denegado para notificaciones", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tipoPlan = arguments?.getString("tipo_plan") ?: "Plan"
        val titulo = if (tipoPlan == "Dieta") "Tu Dieta Personalizada" else "Tu Rutina Personalizada"
        binding.tvPlanTitle.text = titulo

        generarPlanConIA(tipoPlan)

        binding.btnProgramarAlarma.setOnClickListener {
            checkNotificationPermission()
        }

        binding.btnVolver.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun generarPlanConIA(tipo: String) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            //modelName = "gemini-3-flash",
            apiKey = apiKey
        )

        val prompt = buildPrompt(tipo)

        lifecycleScope.launch {
            try {
                binding.tvPlanContent.text = "Generando tu $tipo con IA... Por favor espera."
                val response = generativeModel.generateContent(prompt)
                
                if (response.text != null) {
                    binding.tvPlanContent.text = response.text
                } else {
                    binding.tvPlanContent.text = "La IA no devolvió texto. Intenta nuevamente."
                }
                
            } catch (e: Exception) {
                binding.tvPlanContent.text = "Error: ${e.localizedMessage}"
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleAlarm()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleAlarm()
        }
    }

    private fun scheduleAlarm() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("tipo_plan", tipoPlan)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.SECOND, 10)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Toast.makeText(context, "Recordatorio programado en 10 segundos", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al programar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildPrompt(tipo: String): String {
        val enfermedad = UserSession.enfermedadPreexistente

        val contextoEspecializado = if (tipo == "Dieta") {
            """
            Genera un PLAN DE ALIMENTACIÓN detallado.
            - Desayuno, Colación Mañana, Almuerzo, Colación Tarde y Cena.
            - Incluye macros aproximados.
            REGLAS NUTRICIONALES:
            - Condición Médica: $enfermedad.
            - Si es Diabetes: Evita azúcares simples.
            - Si es Hipertensión: Dieta baja en sodio.
            - Si es Colesterol: Reduce grasas saturadas.
            """.trimIndent()
        } else {
            """
            Genera una RUTINA DE EJERCICIOS semanal.
            - Divide por días.
            - Adapta la intensidad al nivel ${UserSession.nivelActividad} y objetivo ${UserSession.objetivo}.
            - REGLA DE SEGURIDAD: Dado que el usuario tiene $enfermedad, adapta los ejercicios para que sean seguros (especialmente si es hipertensión o problemas articulares).
            """.trimIndent()
        }

        return """
            Actúa como un experto en salud.
            Usuario: ${UserSession.nombres}, ${UserSession.genero}, ${UserSession.edad} años.
            Peso Actual: ${UserSession.pesoActual}kg (Peso Inicial: ${UserSession.pesoInicial}kg).
            Talla: ${UserSession.talla}cm.
            Objetivo: ${UserSession.objetivo}.
            
            SOLICITUD: $contextoEspecializado
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

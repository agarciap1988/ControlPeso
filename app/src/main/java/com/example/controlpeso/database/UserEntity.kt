package com.example.controlpeso.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey val email: String, // Usamos el email como identificador único
    val nombres: String,
    val genero: String,
    val edad: Int,
    val altura: Int,
    val pesoInicial: Float, // Guardado al inicio (fijo)
    var pesoActual: Float,  // Se actualizará con los pesajes semanales
    val enfermedad: String,
    val objetivo: String,
    val actividad: String
)

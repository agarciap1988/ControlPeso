package com.example.controlpeso.database

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Solo actualiza el peso actual conservando el inicial
    @Query("UPDATE usuarios SET pesoActual = :nuevoPeso")
    suspend fun updatePesoActual(nuevoPeso: Float)

    @Query("DELETE FROM usuarios")
    suspend fun deleteUser()
}

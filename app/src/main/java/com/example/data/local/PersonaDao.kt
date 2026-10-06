package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    @Query("SELECT * FROM personas")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    @Query("SELECT * FROM personas WHERE id = :id LIMIT 1")
    suspend fun getPersonaById(id: String): PersonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultPersonas(personas: List<PersonaEntity>)

    @Update
    suspend fun updatePersona(persona: PersonaEntity)

    @Delete
    suspend fun deletePersona(persona: PersonaEntity)
}

package cr.ac.una.glifo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class UserEntity(
    @PrimaryKey val id: Long,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val token: String?
)

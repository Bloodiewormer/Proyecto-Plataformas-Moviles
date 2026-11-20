package cr.ac.una.glifo.user.repository

import cr.ac.una.glifo.user.entity.UserAvatar
import org.springframework.data.jpa.repository.JpaRepository

interface UserAvatarRepository : JpaRepository<UserAvatar, Long>

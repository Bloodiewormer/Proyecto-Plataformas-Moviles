package cr.ac.una.glifo.study.repository

import cr.ac.una.glifo.study.entity.StudyItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StudyItemRepository : JpaRepository<StudyItem, Long> {
    fun findByCourseIdAndSyllabusTopicId(courseId: Long, topicId: Long): List<StudyItem>
    fun findByCourseId(courseId: Long): List<StudyItem>
}

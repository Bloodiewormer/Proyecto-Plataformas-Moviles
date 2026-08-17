package cr.ac.una.glifo.course.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.CreateCourseRequest
import cr.ac.una.glifo.course.dto.EnrollmentResponse
import cr.ac.una.glifo.course.dto.JoinCourseRequest
import cr.ac.una.glifo.course.service.CourseService
import cr.ac.una.glifo.security.GlifoUserDetails
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/courses")
class CourseController(private val courseService: CourseService) {
    
    @PostMapping
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    fun createCourse(
        @Valid @RequestBody request: CreateCourseRequest, 
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<CourseResponse>> {
        val response = courseService.createCourse(request, user.userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = response))
    }
    
    @GetMapping
    fun getCourses(@AuthenticationPrincipal user: GlifoUserDetails): ResponseEntity<ApiResponse<List<CourseResponse>>> {
        val response = courseService.getUserCourses(user.userId)
        return ResponseEntity.ok(ApiResponse(data = response))
    }
    
    @GetMapping("/{id}")
    fun getCourse(@PathVariable id: Long): ResponseEntity<ApiResponse<CourseResponse>> {
        val response = courseService.getCourseById(id)
        return ResponseEntity.ok(ApiResponse(data = response))
    }
}

@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(private val courseService: CourseService) {
    
    @PostMapping
    fun joinCourse(
        @Valid @RequestBody request: JoinCourseRequest, 
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<EnrollmentResponse>> {
        val response = courseService.joinCourse(request.joinCode, user.userId)
        return ResponseEntity.ok(ApiResponse(data = response))
    }
}

package cr.ac.una.glifo.course.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.CreateCourseRequest
import cr.ac.una.glifo.course.dto.EnrollmentResponse
import cr.ac.una.glifo.course.dto.JoinCourseRequest
import cr.ac.una.glifo.course.service.CourseService
import cr.ac.una.glifo.security.GlifoUserDetails
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * | Método | Ruta          | Éxito | Errores                              |
 * |--------|---------------|-------|--------------------------------------|
 * | GET    | /courses      | 200   | 401                                  |
 * | GET    | /courses/{id} | 200   | 400, 404                             |
 * | POST   | /courses      | 201   | 400, 403 (sin COURSE_WRITE), 415     |
 */
@RestController
@RequestMapping("\${url.courses}", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourseController(private val courseService: CourseService) {

    @GetMapping
    fun getCourses(@AuthenticationPrincipal user: GlifoUserDetails): ResponseEntity<ApiResponse<List<CourseResponse>>> =
        ResponseEntity.ok(ApiResponse(data = courseService.getUserCourses(user.userId)))

    @GetMapping("/{id}")
    fun getCourse(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") id: Long
    ): ResponseEntity<ApiResponse<CourseResponse>> =
        ResponseEntity.ok(ApiResponse(data = courseService.getCourseById(id)))

    /** 201 Created con la ubicación del curso nuevo en el encabezado Location. */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    fun createCourse(
        @Valid @RequestBody request: CreateCourseRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<CourseResponse>> {
        val created = courseService.createCourse(request, user.userId)
        val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .path("/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(ApiResponse(data = created))
    }
}

/**
 * | Método | Ruta         | Éxito | Errores                                      |
 * |--------|--------------|-------|----------------------------------------------|
 * | POST   | /enrollments | 201   | 400, 404 (código inválido), 409 (ya inscrito) |
 */
@RestController
@RequestMapping("\${url.enrollments}", produces = [MediaType.APPLICATION_JSON_VALUE])
class EnrollmentController(private val courseService: CourseService) {

    /** Crea una matrícula, así que responde 201 Created (antes respondía 200). */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun joinCourse(
        @Valid @RequestBody request: JoinCourseRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<EnrollmentResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = courseService.joinCourse(request.joinCode, user.userId)))
}

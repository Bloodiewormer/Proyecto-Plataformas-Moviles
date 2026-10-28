package cr.ac.una.glifo.note.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.note.dto.CreateNotePageRequest
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NotePageResponse
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.dto.UpdateNoteRequest
import cr.ac.una.glifo.note.dto.UpdateNoteStatusRequest
import cr.ac.una.glifo.note.service.NoteService
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
 * Apuntes del usuario autenticado. El dueño siempre sale del token (@AuthenticationPrincipal),
 * nunca del cuerpo de la petición.
 *
 * | Método | Ruta                  | Éxito | Errores                     |
 * |--------|-----------------------|-------|-----------------------------|
 * | GET    | /notes?courseId=      | 200   | 400                         |
 * | GET    | /notes/{id}           | 200   | 400, 404                    |
 * | POST   | /notes                | 201   | 400, 404 (curso), 415       |
 * | PUT    | /notes/{id}           | 200   | 400, 404, 415               |
 * | PATCH  | /notes/{id}/status    | 200   | 400, 404, 409 (transición)  |
 * | DELETE | /notes/{id}           | 204   | 400, 404                    |
 * | POST   | /notes/{id}/pages     | 201   | 400, 404, 409 (duplicada)   |
 * | GET    | /notes/{id}/pages     | 200   | 400, 404                    |
 */
@RestController
@RequestMapping("\${url.notes}", produces = [MediaType.APPLICATION_JSON_VALUE])
class NoteController(
    private val noteService: NoteService
) {

    @GetMapping
    @PreAuthorize("hasAuthority('NOTE_READ_OWN')")
    fun getNotes(
        @RequestParam(required = false) @Positive(message = "El curso no es válido") courseId: Long?,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<List<NoteResponse>>> =
        ResponseEntity.ok(ApiResponse(data = noteService.getNotesByUser(user.userId, courseId)))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTE_READ_OWN')")
    fun getNote(
        @PathVariable @Positive(message = "El id del apunte debe ser positivo") id: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<NoteResponse>> =
        ResponseEntity.ok(ApiResponse(data = noteService.getNoteById(id, user.userId)))

    /** 201 Created con la ubicación del recurso nuevo en el encabezado Location. */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('NOTE_WRITE_OWN')")
    fun createNote(
        @Valid @RequestBody request: CreateNoteRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<NoteResponse>> {
        val created = noteService.createNote(user.userId, request)
        val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .path("/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(ApiResponse(data = created))
    }

    @PutMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('NOTE_WRITE_OWN')")
    fun updateNote(
        @PathVariable @Positive(message = "El id del apunte debe ser positivo") id: Long,
        @Valid @RequestBody request: UpdateNoteRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<NoteResponse>> =
        ResponseEntity.ok(ApiResponse(data = noteService.updateNote(id, user.userId, request)))

    @PatchMapping("/{id}/status", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('NOTE_WRITE_OWN')")
    fun updateNoteStatus(
        @PathVariable @Positive(message = "El id del apunte debe ser positivo") id: Long,
        @Valid @RequestBody request: UpdateNoteStatusRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<NoteResponse>> =
        ResponseEntity.ok(ApiResponse(data = noteService.updateNoteStatus(id, user.userId, request.status)))

    /** 204 No Content: se borró y no hay nada que devolver. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTE_DELETE_OWN')")
    fun deleteNote(
        @PathVariable @Positive(message = "El id del apunte debe ser positivo") id: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<Void> {
        noteService.deleteNote(id, user.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/pages", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('NOTE_WRITE_OWN')")
    fun uploadPage(
        @PathVariable @Positive(message = "El id del apunte debe ser positivo") id: Long,
        @Valid @RequestBody request: CreateNotePageRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<NotePageResponse>> =
        // Sin Location: una página no tiene un GET propio, se consulta con GET /notes/{id}/pages.
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = noteService.addPage(id, user.userId, request)))

    @GetMapping("/{id}/pages")
    @PreAuthorize("hasAuthority('NOTE_READ_OWN')")
    fun getPages(
        @PathVariable @Positive(message = "El id del apunte debe ser positivo") id: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<List<NotePageResponse>>> =
        ResponseEntity.ok(ApiResponse(data = noteService.getPages(id, user.userId)))

    @PostMapping("/{id}/reconstruct")
    @PreAuthorize("hasAuthority('NOTE_WRITE_OWN')")
    fun reconstructNote(
        @PathVariable @Positive(message = "El id del apunte debe ser positivo") id: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<NoteResponse>> {
        val outcome = noteService.reconstructNote(id, user.userId)
        return ResponseEntity.ok(
            ApiResponse(
                data = outcome.note,
                meta = mapOf("reconstructed" to outcome.reconstructed)
            )
        )
    }
}

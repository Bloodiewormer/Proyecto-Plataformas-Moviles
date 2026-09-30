package cr.ac.una.glifo.note.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NotePageResponse
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.service.NoteService
import cr.ac.una.glifo.security.GlifoUserDetails
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/notes")
class NoteController(
    private val noteService: NoteService
) {

    @PostMapping
    fun createNote(
        @Valid @RequestBody request: CreateNoteRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<NoteResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = noteService.createNote(user.userId, request)))

    @GetMapping
    fun getNotes(
        @RequestParam(required = false) courseId: Long?,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ApiResponse<List<NoteResponse>> =
        ApiResponse(data = noteService.getNotesByUser(user.userId, courseId))

    @GetMapping("/{id}")
    fun getNote(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ApiResponse<NoteResponse> =
        ApiResponse(data = noteService.getNoteById(id, user.userId))

    @DeleteMapping("/{id}")
    fun deleteNote(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<Void> {
        noteService.deleteNote(id, user.userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/status")
    fun updateNoteStatus(
        @PathVariable id: Long,
        @RequestBody request: Map<String, String>,
        @AuthenticationPrincipal user: GlifoUserDetails
    ) {
        val newStatus = request["status"] ?: throw IllegalArgumentException("Status is required")
        noteService.updateNoteStatus(id, user.userId, newStatus)
    }

    @PostMapping("/{id}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPage(
        @PathVariable id: Long,
        @RequestParam("pageIndex") pageIndex: Int,
        @RequestParam("file") file: MultipartFile
    ): ApiResponse<NotePageResponse> {
        // Placeholder for uploading a page
        return ApiResponse(
            data = NotePageResponse(
                id = 1L,
                noteId = id,
                pageIndex = pageIndex,
                storageUri = "placeholder/uri",
                levelReached = "N0",
                overallConfidence = 0.0f,
                processedAt = null
            )
        )
    }

    @GetMapping("/{id}/pages")
    fun getPages(@PathVariable id: Long): ApiResponse<List<NotePageResponse>> {
        // Placeholder for listing pages
        return ApiResponse(data = emptyList())
    }
}

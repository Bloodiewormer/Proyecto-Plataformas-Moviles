package cr.ac.una.glifo.note.controller

import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NotePageResponse
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.service.NoteService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val noteService: NoteService
) {

    // Placeholder for actual authentication
    private fun getCurrentUserId(): Long = 1L

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createNote(@RequestBody request: CreateNoteRequest): NoteResponse {
        return noteService.createNote(getCurrentUserId(), request)
    }

    @GetMapping
    fun getNotes(
        @RequestParam(required = false) courseId: Long?
    ): List<NoteResponse> {
        return noteService.getNotesByUser(getCurrentUserId(), courseId)
    }

    @GetMapping("/{id}")
    fun getNote(@PathVariable id: Long): NoteResponse {
        return noteService.getNoteById(id)
    }

    @PatchMapping("/{id}/status")
    fun updateNoteStatus(
        @PathVariable id: Long,
        @RequestBody request: Map<String, String>
    ) {
        val newStatus = request["status"] ?: throw IllegalArgumentException("Status is required")
        noteService.updateNoteStatus(id, newStatus)
    }

    @PostMapping("/{id}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPage(
        @PathVariable id: Long,
        @RequestParam("pageIndex") pageIndex: Int,
        @RequestParam("file") file: MultipartFile
    ): NotePageResponse {
        // Placeholder for uploading a page
        return NotePageResponse(
            id = 1L,
            noteId = id,
            pageIndex = pageIndex,
            storageUri = "placeholder/uri",
            levelReached = "N0",
            overallConfidence = 0.0f,
            processedAt = null
        )
    }

    @GetMapping("/{id}/pages")
    fun getPages(@PathVariable id: Long): List<NotePageResponse> {
        // Placeholder for listing pages
        return emptyList()
    }
}

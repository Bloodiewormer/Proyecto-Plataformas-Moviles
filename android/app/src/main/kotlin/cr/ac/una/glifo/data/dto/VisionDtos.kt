package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

/** Respuesta de `POST notes/{id}/pages/{pageIndex}/regions/{regionId}/math` (#38 / #39). */
data class MathOcrResponseDto(
    @SerializedName(value = "pageIndex", alternate = ["page_index"]) val pageIndex: Int = 0,
    @SerializedName(value = "regionId", alternate = ["region_id"]) val regionId: String = "",
    @SerializedName("latex") val latex: String? = null,
    @SerializedName("confidence") val confidence: Float = 0f,
    @SerializedName(value = "latexCompiles", alternate = ["latex_compiles"]) val latexCompiles: Boolean = false,
    @SerializedName("status") val status: String = "",
    @SerializedName(value = "resolvedAt", alternate = ["resolved_at"]) val resolvedAt: String = "UNRESOLVED",
    @SerializedName(value = "escalationReasons", alternate = ["escalation_reasons"]) val escalationReasons: List<String> = emptyList(),
    @SerializedName("engine") val engine: String? = null,
    @SerializedName("cached") val cached: Boolean = false
)

/** Respuesta de `POST notes/{id}/pages/{pageIndex}/repair` (#41). */
data class PageRepairResponseDto(
    @SerializedName(value = "pageIndex", alternate = ["page_index"]) val pageIndex: Int = 0,
    @SerializedName(value = "levelReached", alternate = ["level_reached"]) val levelReached: String = "N2",
    @SerializedName(value = "repaired", alternate = ["repaired_count"]) val repaired: Int = 0,
    @SerializedName(value = "uncertain", alternate = ["unresolved_count"]) val uncertain: Int = 0,
    @SerializedName("regions") val regions: List<RepairedRegionDto> = emptyList()
)

data class RepairedRegionDto(
    @SerializedName(value = "regionId", alternate = ["region_id"]) val regionId: String = "",
    @SerializedName("kind") val kind: String = "",
    @SerializedName("value") val value: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("latex") val latex: String? = null,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName(value = "latexCompiles", alternate = ["latex_compiles"]) val latexCompiles: Boolean? = null,
    @SerializedName("confidence") val confidence: Float = 0f,
    @SerializedName(value = "resolvedAt", alternate = ["resolved_at"]) val resolvedAt: String = "UNRESOLVED",
    @SerializedName(value = "isUncertain", alternate = ["is_uncertain"]) val isUncertain: Boolean = false
) {
    val resolvedTextOrValue: String?
        get() = value ?: latex ?: text ?: caption
}

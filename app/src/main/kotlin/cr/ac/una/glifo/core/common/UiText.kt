package cr.ac.una.glifo.core.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class Plain(val value: String) : UiText

    class Resource(@StringRes val resId: Int, vararg val args: Any) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Resource) return false
            return resId == other.resId && args.contentEquals(other.args)
        }

        override fun hashCode(): Int = 31 * resId + args.contentHashCode()
    }

    fun resolve(context: Context): String = when (this) {
        is Plain -> value
        is Resource -> if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Resource -> if (args.isEmpty()) stringResource(resId) else stringResource(resId, *args)
}

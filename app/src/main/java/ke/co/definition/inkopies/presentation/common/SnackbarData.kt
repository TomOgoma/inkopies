package ke.co.definition.inkopies.presentation.common

import android.support.design.widget.Snackbar
import android.view.View
import ke.co.definition.inkopies.R

/**
 * Created by tomogoma
 * On 28/02/18.
 */
sealed class SnackbarData {
    abstract fun show(v: View): Snackbar

    internal fun show(sb: Snackbar): Snackbar {
        if (sb.duration == Snackbar.LENGTH_INDEFINITE) {
            sb.setAction(R.string.dismiss, { sb.dismiss() })
            sb.show()
        } else {
            sb.show()
        }
        return sb
    }
}

class ResIDSnackbarData(
        private val resID: Int,
        private val duration: Int
) : SnackbarData() {
    override fun show(v: View) = show(Snackbar.make(v, resID, duration))
}

class TextSnackbarData(
        private val text: String,
        private val duration: Int
) : SnackbarData() {

    constructor(error: Throwable, duration: Int) : this(error.message ?: "", duration)

    override fun show(v: View) = show(Snackbar.make(v, text, duration))
}
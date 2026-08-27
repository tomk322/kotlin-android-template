package com.ncorti.kotlin.template.library.android

import android.content.Context
import android.widget.Toast

object ToastUtil {

    // Lint's ShowToast check cannot follow show() through `also`, so it reports a false positive
    // here. The Toast is both shown and returned, letting callers cancel or re-show it.
    @Suppress("ShowToast")
    fun showToast(context: Context, message: String): Toast =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).also {
            it.show()
        }
}

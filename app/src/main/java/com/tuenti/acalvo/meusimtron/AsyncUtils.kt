package com.tuenti.acalvo.meusimtron

import android.os.AsyncTask


class doAsync(val handler: () -> Unit) : AsyncTask<String, Int, Unit>() {
    override fun doInBackground(vararg params: String) = handler()
}

package com.tuenti.acalvo.meusimtron.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.tuenti.acalvo.meusimtron.R
import com.tuenti.acalvo.meusimtron.SimInfo

class SimActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sim)

        val icc = intent.getStringExtra("icc")
        val simIndex = intent.getIntExtra("sim", -1)

        val layout = findViewById<TextView>(R.id.editText1)
        layout.text = icc

        val saveButton = findViewById<Button>(R.id.button)
        saveButton.setOnClickListener {
            val simInfo = validate();
            if (simInfo == null) {
                Toast.makeText(this@SimActivity, "Fill msisdn", Toast.LENGTH_SHORT).show();
            } else {

            }
        }
    }

    private fun validate(): SimInfo? {
        return null
    }
}

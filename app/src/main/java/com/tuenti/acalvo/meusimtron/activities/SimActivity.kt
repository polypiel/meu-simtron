package com.tuenti.acalvo.meusimtron.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.tuenti.acalvo.meusimtron.*

class SimActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sim)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Add Sim"

        val icc = intent.getStringExtra("icc")


        val iccText = findViewById<TextView>(R.id.editText1)
        iccText.text = icc

        val msisdnText = findViewById<EditText>(R.id.editText2)


        val providerSpinner = findViewById<Spinner>(R.id.spinner3)
        providerSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Provider.values())


        val paymentModelSpinner = findViewById<Spinner>(R.id.spinner4)
        paymentModelSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                PaymentModel.values())

        val sim = Directory.instance.getSimInfo(icc)!!.simInfo
        if (sim != null) {
            msisdnText.setText(sim.msisdn, TextView.BufferType.EDITABLE)
            providerSpinner.setSelection(Provider.values().indexOf(sim.provider))
            paymentModelSpinner.setSelection(PaymentModel.values().indexOf(sim.paymentModel))
        }

        val saveButton = findViewById<Button>(R.id.button)
        saveButton.setOnClickListener {
            val simInfo = validate()
            if (simInfo == null) {
                Toast.makeText(
                        this@SimActivity,
                        "Error: Missing msisdn",
                        Toast.LENGTH_SHORT).show()
            } else {
                AppManager.INSTANCE.onSimUpdated(icc, simInfo)
                // TODO update notification
                // TODO send slack notification
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    private fun validate(): SimInfo? {
        val msisdn = findViewById<EditText>(R.id.editText2).text.toString()
        return if (!msisdn.isEmpty()) SimInfo(
                msisdn = msisdn,
                provider = findViewById<Spinner>(R.id.spinner3).selectedItem as Provider,
                paymentModel = findViewById<Spinner>(R.id.spinner4).selectedItem as PaymentModel)
        else null
    }
}

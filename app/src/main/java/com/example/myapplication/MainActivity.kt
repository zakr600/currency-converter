package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        currency_from.setSelection(sharedPref.getInt("selected_from", 0))
        currency_to.setSelection(sharedPref.getInt("selected_to", 1))
//        input.setText(sharedPref.getString("input_number", "0"))

        last_updated.text = sharedPref.getString("last_update", getString(R.string.last_update) + ": " + getString(R.string.create_date))

        convert.setOnClickListener {
            try {
                if (input.text.toString().length > 20) throw Exception()
                val from: Double = input.text.toString().toDouble()
                val base = from / sharedPref.getFloat(currency_from.selectedItem.toString(), resources.getStringArray(R.array.currency_rates)[currency_from.selectedItemId.toInt()].toFloat())
                val to = base * sharedPref.getFloat(currency_to.selectedItem.toString(), resources.getStringArray(R.array.currency_rates)[currency_to.selectedItemId.toInt()].toFloat())
                res.text = String.format("%.2f", to)
                val precision = 2
                res.text = "${from.format(precision)} ${currency_from.selectedItem} = ${to.format(precision)} ${currency_to.selectedItem}"
            }
            catch (e : Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, R.string.invalid_input, Toast.LENGTH_LONG).show()
//                Toast.makeText(applicationContext, e.stackTrace.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        Thread(Runnable {
            val connection = URL("https://api.exchangeratesapi.io/latest").openConnection() as HttpsURLConnection
            val data = connection.inputStream.bufferedReader().readText()
            //Toast.makeText(applicationContext, data, Toast.LENGTH_LONG).show()
            val editor = sharedPref.edit()

            val jsonObject = JSONObject(data)
            val rates = jsonObject.getJSONObject("rates")
            for (i in resources.getStringArray(R.array.currency_names)) {
                if (i == "EUR") continue
                val rate = rates.getString(i).toFloat()
                editor.putFloat(i, rate)
            }

            editor.putString("last_update", jsonObject.getString("date"))
            Handler(Looper.getMainLooper()).post(Runnable { last_updated.text = getString(R.string.last_update) + ": " + jsonObject.getString("date") })

            editor.apply()
        }).start()
    }

    override fun onStop() {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
//        editor.putString("input_number", input.text.toString())
        editor.putInt("selected_from", currency_from.selectedItemPosition)
        editor.putInt("selected_to", currency_to.selectedItemPosition)
//        Log.i("Zakhar", "Saved")
        editor.commit()

        super.onStop()
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
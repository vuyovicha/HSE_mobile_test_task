package com.example.firststage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Request
import okhttp3.Callback

import java.io.IOException
import java.util.ArrayList
import kotlin.math.round



class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val currencyFileTagSize = 3
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapCurrencies = setSpinners()

        findViewById<Button>(R.id.button).setOnClickListener {
            val inputNumberView = findViewById<EditText>(R.id.input_number)
            val inputText = inputNumberView.text.toString()
            val inputLength = inputText.length
            if (inputLength > 0) {
                val fromTag = mapCurrencies[from_spinner.selectedItem.toString()].toString()
                val toTag = mapCurrencies[to_spinner.selectedItem.toString()].toString()
                if (fromTag != toTag) {
                    val url = "https://api.exchangeratesapi.io/latest?base=$fromTag"
                    val request = Request.Builder()
                        .url(url)
                        .build()
                    client.newCall(request).enqueue(object: Callback {
                        override fun onFailure(call: okhttp3.Call, e: IOException) { }
                        override fun onResponse(call: okhttp3.Call, response: Response) {
                            val requestedString = response.body()?.string().toString()
                            val rate = getNeededRate(requestedString, toTag)
                            val fromAmount = inputText.toDouble()
                            val toAmount = round(fromAmount * rate * 100) / 100
                            this@MainActivity.runOnUiThread(java.lang.Runnable {
                                findViewById<TextView>(R.id.output_value).text = toAmount.toString()
                            })
                        }
                    })
                } else {
                    val sameCurrenciesToast = Toast.makeText(this@MainActivity, getString(R.string.same_currencies_toast), Toast.LENGTH_SHORT)
                    sameCurrenciesToast.show()
                    findViewById<TextView>(R.id.output_value).text = "-"

                }
            } else {
                val noAmountToast = Toast.makeText(this@MainActivity, getString(R.string.no_amount_toast), Toast.LENGTH_SHORT)
                noAmountToast.show()
                findViewById<TextView>(R.id.output_value).text = "-"
            }

        }
    }

    private fun readFileText(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun getNeededRate(requestedString: String, toTag: String): Double {
        val distanceToRate = 5 //taken from request
        val separator = ','
        //not constraining the ending index because there are no needed tags in the end of the request
        for (i in 0 until requestedString.length - 3) {
            val currentTrio = requestedString[i].toString() + requestedString[i + 1].toString() + requestedString[i + 2].toString()
            if (currentTrio == toTag) {
                var currentIndex = i + distanceToRate
                while (requestedString[currentIndex] != separator) {
                    currentIndex++
                }
                val rateStartsAt = i + distanceToRate
                val rateEndsAt = currentIndex
                return requestedString.substring(rateStartsAt, rateEndsAt).toDouble()
            }
        }
        return -1.0
    }

    private fun setSpinners(): Map<String, String> {
        val getFileString = readFileText("currencies_tags.txt")
        var lines = getFileString.lines()
        val mapCurrencies = mutableMapOf<String, String>()
        val tagsForSpinner : MutableList<String> = ArrayList()
        lines.forEach {
            if (it.isNotEmpty()) {
                val tag = it.take(currencyFileTagSize)
                val name = it.takeLast(it.length - currencyFileTagSize - 1)
                mapCurrencies.put(name, tag)
                tagsForSpinner.add(name)
            }
        }
        val spinnerAdapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, tagsForSpinner)
        from_spinner.adapter = spinnerAdapter
        to_spinner.adapter = spinnerAdapter
        to_spinner.setSelection(1)
        return mapCurrencies
    }

}
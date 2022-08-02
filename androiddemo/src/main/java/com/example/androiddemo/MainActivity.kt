package com.example.androiddemo

import androidx.appcompat.app.AppCompatActivity
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Felt
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val result = StarknetCurve.pedersen(Felt(1), Felt(2))
        val text = findViewById<TextView>(R.id.HASH_VIEW)
        text.setText("PEDERSEN RESULT "+ result.toString())
    }
}

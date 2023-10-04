package com.onixen.audioplayer.views.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.ActivityMainBinding
import com.onixen.audioplayer.views.fragments.PlayerFragment

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(binding.fragmentContainer.id, PlayerFragment.getInstance())
        transaction.commit()
    }
}
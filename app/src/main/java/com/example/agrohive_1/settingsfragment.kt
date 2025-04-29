package com.example.agrohive_1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsFragment : Fragment() {

    private lateinit var radioGroupTheme: RadioGroup
    private lateinit var radioGroupFont: RadioGroup
    private lateinit var settingsLayout: LinearLayout
    private lateinit var themeLabel: TextView
    private lateinit var fontLabel: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize views
        settingsLayout = view.findViewById(R.id.settings_layout)
        radioGroupTheme = view.findViewById(R.id.radioGroupTheme)
        radioGroupFont = view.findViewById(R.id.radioGroupFont)
        themeLabel = view.findViewById(R.id.theme_label)
        fontLabel = view.findViewById(R.id.font_label)

        // Load saved preferences
        val sharedPrefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedTheme = sharedPrefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val savedFont = sharedPrefs.getString("fontSize", "medium") ?: "medium"
        val userType = sharedPrefs.getString("userType", "Customer") ?: "Customer"

        // Set radio buttons
        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> radioGroupTheme.check(R.id.radioLight)
            AppCompatDelegate.MODE_NIGHT_YES -> radioGroupTheme.check(R.id.radioDark)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> radioGroupTheme.check(R.id.radioSystem)
        }
        when (savedFont) {
            "small" -> radioGroupFont.check(R.id.radioFontSmall)
            "medium" -> radioGroupFont.check(R.id.radioFontMedium)
            "large" -> radioGroupFont.check(R.id.radioFontLarge)
        }

        // Apply initial font size and background
        applyFontSize(savedFont)
        applyBackground(savedTheme)

        // Set navigation menu based on user type
        setupNavigationMenu(userType)

        // Theme change listener
        radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.radioLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioDark -> AppCompatDelegate.MODE_NIGHT_YES
                R.id.radioSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(newTheme)
            sharedPrefs.edit().putInt("theme", newTheme).apply()
            applyBackground(newTheme)
            requireActivity().recreate()
        }

        // Font size change listener
        radioGroupFont.setOnCheckedChangeListener { _, checkedId ->
            val newFont = when (checkedId) {
                R.id.radioFontSmall -> "small"
                R.id.radioFontMedium -> "medium"
                R.id.radioFontLarge -> "large"
                else -> "medium"
            }
            sharedPrefs.edit().putString("fontSize", newFont).apply()
            applyFontSize(newFont)
            requireActivity().recreate()
        }

        return view
    }

    private fun applyFontSize(fontSize: String) {
        val textSize = when (fontSize) {
            "small" -> 14f
            "medium" -> 16f
            "large" -> 18f
            else -> 16f
        }
        themeLabel.textSize = textSize
        fontLabel.textSize = textSize
        // Apply to RadioButtons
        radioGroupTheme.findViewById<RadioButton>(R.id.radioLight).textSize = textSize
        radioGroupTheme.findViewById<RadioButton>(R.id.radioDark).textSize = textSize
        radioGroupTheme.findViewById<RadioButton>(R.id.radioSystem).textSize = textSize
        radioGroupFont.findViewById<RadioButton>(R.id.radioFontSmall).textSize = textSize
        radioGroupFont.findViewById<RadioButton>(R.id.radioFontMedium).textSize = textSize
        radioGroupFont.findViewById<RadioButton>(R.id.radioFontLarge).textSize = textSize
    }

    private fun applyBackground(theme: Int) {
        val backgroundColor = when (theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> ContextCompat.getColor(requireContext(), android.R.color.white)
            AppCompatDelegate.MODE_NIGHT_YES -> ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> ContextCompat.getColor(requireContext(), android.R.color.white)
            else -> ContextCompat.getColor(requireContext(), android.R.color.white)
        }
        settingsLayout.setBackgroundColor(backgroundColor)
    }

    private fun setupNavigationMenu(userType: String) {
        // Find BottomNavigationView in the host activity
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav?.let {
            // Clear existing menu to avoid duplication
            it.menu.clear()
            // Inflate menu based on user type
            val menuRes = when (userType.lowercase()) {
                "farmer" -> R.menu.bottom_nav_menu
                "customer", "marketer" -> R.menu.bottom_nav_menu_customer
                else -> R.menu.bottom_nav_menu_customer // Default to customer
            }
            it.inflateMenu(menuRes)
        }
    }
}
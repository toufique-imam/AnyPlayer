package com.stream.jmxplayer.ui.fragment

import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import com.stream.jmxplayer.R
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.SharedPreferenceUtils

class SettingsFragment : PreferenceFragmentCompat() {

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_settings, container, false)
//    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        logger("sharedPref", rootKey + "")
        setPreferencesFromResource(R.xml.settings_pref, rootKey)
        val dropDownPreference: DropDownPreference? = findPreference(SharedPreferenceUtils.THEME)
        dropDownPreference?.summary = SharedPreferenceUtils.getUserTheme(requireContext())
        dropDownPreference?.setOnPreferenceChangeListener { _, newValue ->
            logger("OnPrefChange", "$newValue")
            dropDownPreference.summary = newValue as String?
            SharedPreferenceUtils.setUserTheme(requireContext(), newValue as String)
            requireActivity().recreate()
            true
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            SettingsFragment().apply {
                arguments = Bundle()
            }
    }
}
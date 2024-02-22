package com.retroline.anyplayer.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.retroline.anyplayer.R
import com.retroline.anyplayer.utils.GlobalFunctions.logger
import com.retroline.anyplayer.utils.Settings


class SettingsFragment : PreferenceFragmentCompat() {
    lateinit var values: Array<String>
    private lateinit var formats: Array<String>

    private fun getPixelValue(string: String): String {
        for ((idx, i) in values.withIndex()) {
            if (i == string) {
                return formats[idx]
            }
        }
        return formats[0]
    }

    private lateinit var mSettings: Settings
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        logger("sharedPref", rootKey + "")
        values = resources.getStringArray(R.array.pref_entry_values_pixel_format)
        formats = resources.getStringArray(R.array.pref_entry_summaries_pixel_format)
        mSettings = Settings(requireContext())

        setPreferencesFromResource(R.xml.settings_pref, rootKey)

        val themeDropDown: ListPreference? = findPreference(getString(R.string.pref_key_theme))
        themeDropDown?.value = mSettings.theme.toString()
        themeDropDown?.summary = themeDropDown?.entries?.get(mSettings.theme)
        themeDropDown?.setOnPreferenceChangeListener { _, newValue ->
            logger("OnPrefChangeTheme", "$newValue")
            //SharedPreferenceUtils.setUserTheme(requireContext(), newValue as String)
            val value = (newValue).toString()
            if (mSettings.theme.toString() != value) {
                mSettings.setTheme(value)
                requireActivity().recreate()
            }
            true
        }
        val pixelFormat: ListPreference? =
            findPreference(getString(R.string.pref_key_pixel_format))
        pixelFormat?.value = mSettings.pixelFormat
        pixelFormat?.summary = getPixelValue(mSettings.pixelFormat)
        pixelFormat?.setOnPreferenceChangeListener { _, newValue ->
            logger("OnPrefChangePixel", "$newValue")
            pixelFormat.summary = getPixelValue(newValue.toString())
            mSettings.pixelFormat = newValue.toString()
            true
        }
        val resolution =
            findPreference<SwitchPreference>(getString(R.string.pref_key_media_codec_handle_resolution_change))
        resolution?.setDefaultValue(mSettings.mediaCodecHandleResolutionChange)
        resolution?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                logger("OnPrefChangeResolution", "$newValue")
                mSettings.mediaCodecHandleResolutionChange = newValue as Boolean
                true
            }
        val rotate =
            findPreference<SwitchPreference>(getString(R.string.pref_key_using_media_codec_auto_rotate))
        rotate?.setDefaultValue(mSettings.usingMediaCodecAutoRotate)
        rotate?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            logger("OnPrefChangeRotate", "$newValue")
            mSettings.usingMediaCodecAutoRotate = newValue as Boolean
            true
        }
        val mediaCodec =
            findPreference<SwitchPreference>(getString(R.string.pref_key_using_media_codec))
        mediaCodec?.setDefaultValue(mSettings.usingMediaCodec)

        mediaCodec?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                logger("OnPrefChangeMediaCodec", "$newValue")
                mSettings.usingMediaCodec = newValue as Boolean
                true
            }
        val opensles =
            findPreference<SwitchPreference>(getString(R.string.pref_key_using_opensl_es))
        opensles?.setDefaultValue(mSettings.usingOpenSLES)
        logger("usingOpenSLES", "${mSettings.usingOpenSLES}")
        opensles?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                logger("usingOpenSLES", "$newValue")
                mSettings.usingOpenSLES = newValue as Boolean
                true
            }

        val renderMode = findPreference<ListPreference>(getString(R.string.pref_key_render))
        renderMode?.value = mSettings.renderExo.toString()
        renderMode?.summary = renderMode?.entries?.get(mSettings.renderExo)
        renderMode?.setOnPreferenceChangeListener { _, newValue ->
            logger("OnPrefChangeExo", "$newValue")
            renderMode.summary = renderMode.entries[Integer.parseInt(newValue.toString())]
            mSettings.setRenderExo(newValue.toString())
            true
        }

        val playerSelect =
            findPreference<ListPreference>(getString(R.string.pref_key_player_select))
        playerSelect?.value = mSettings.player.toString()
        playerSelect?.summary = playerSelect?.entries?.get(mSettings.player)
        playerSelect?.setOnPreferenceChangeListener { _, newValue ->
            playerSelect.summary = playerSelect.entries[Integer.parseInt(newValue.toString())]
            logger("OnPrefChangePlayer", "$newValue ${playerSelect.summary}")
            mSettings.setPlayer(newValue.toString())
            true
        }
        val buttonTerms: Preference? = findPreference(getString(R.string.any_terms))
        buttonTerms?.setOnPreferenceClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://anyplayer.vercel.app/Terms.html"))
            startActivity(browserIntent)
            true
        }
        val buttonPolicy: Preference? = findPreference(getString(R.string.any_policy))
        buttonPolicy?.setOnPreferenceClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://anyplayer.vercel.app/Policy.html"))
            startActivity(browserIntent)
            true
        }
        val buttonMail: Preference? = findPreference(getString(R.string.any_support))
        buttonMail?.setOnPreferenceClickListener {
            val browserIntent = Intent(Intent.ACTION_SENDTO)
            browserIntent.data = Uri.parse("mailto:") // only email apps should handle this
            browserIntent.putExtra(Intent.EXTRA_EMAIL, "2013nuhash@gmail.com")
            if (browserIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(browserIntent)
            }
            startActivity(browserIntent)
            true
        }
        val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val offlineVersion = pInfo.versionName
        val buttonVersion: Preference? = findPreference(getString(R.string.any_version_key))
        buttonVersion?.summary = offlineVersion

    }
}
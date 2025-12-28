// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.ai

import android.content.res.Resources
import android.view.View
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.SettingsValues
import helium314.keyboard.latin.utils.ResourceUtils

/**
 * AI keyboard layout parameters similar to ClipboardLayoutParams and EmojiLayoutParams
 */
class AiLayoutParams(res: Resources) {
    val aiKeyboardHeight: Int
    val bottomRowKeyboardHeight: Int
    private val aiActionButtonHeight: Int

    init {
        val sv = Settings.getValues()
        aiKeyboardHeight = ResourceUtils.getSecondaryKeyboardHeight(res, sv)
        bottomRowKeyboardHeight = 0
        aiActionButtonHeight = res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
    }



    fun setListProperties(view: View) {
        val layoutParams = view.layoutParams
        layoutParams.height = aiKeyboardHeight
        view.layoutParams = layoutParams
    }
}

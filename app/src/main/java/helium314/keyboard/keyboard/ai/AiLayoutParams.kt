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
    val totalKeyboardHeight: Int

    init {
        val sv = Settings.getValues()
        // The main keyboard height consists of the keys area + the suggestions strip area.
        // Secondary keyboards (like AI) hide the strip, so we must add its height back to match the total.
        val keyboardHeight = ResourceUtils.getKeyboardHeight(res, sv)
        val stripHeight = res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
        
        totalKeyboardHeight = keyboardHeight + stripHeight
    }
}

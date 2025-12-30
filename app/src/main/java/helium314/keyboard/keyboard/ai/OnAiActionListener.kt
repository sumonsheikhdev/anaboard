// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.ai

/**
 * Listener interface for AI action callbacks
 */
interface OnAiActionListener {
    /**
     * Called when user requests to improve text
     */
    fun onImproveText(text: String)

    /**
     * Called when user requests to fix grammar
     */
    fun onFixGrammar(text: String)

    /**
     * Called when user requests to translate text
     */
    fun onTranslate(text: String, targetLanguage: String = "en")

    /**
     * Get the current text from the editor
     */
    fun onGetCurrentText(): String

    /**
     * Replace the current text in the input field
     */
    fun onReplaceText(text: String)

}

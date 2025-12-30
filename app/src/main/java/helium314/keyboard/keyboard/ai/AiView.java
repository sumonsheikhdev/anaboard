// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.ai;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import helium314.keyboard.ApiClient;
import helium314.keyboard.AuthManager;
import helium314.keyboard.LoginActivity;
import helium314.keyboard.event.HapticEvent;
import helium314.keyboard.keyboard.KeyboardActionListener;
import helium314.keyboard.keyboard.internal.KeyDrawParams;
import helium314.keyboard.keyboard.internal.KeyVisualAttributes;
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode;
import helium314.keyboard.latin.AudioAndHapticFeedbackManager;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.utils.Log;

import org.json.JSONObject;

@SuppressLint("CustomViewStyleable")
public class AiView extends LinearLayout implements View.OnClickListener {

    // Constants
    private static final String TAG = "AiView";
    private static final String DEFAULT_LANGUAGE = "Bangla";
    private static final String DEFAULT_TONE = "Polite";
    private static final String DEFAULT_STYLE = "Professional";
    private static final int COLOR_DEFAULT = Color.parseColor("#3F51B5");
    private static final int COLOR_SELECTED = Color.parseColor("#FF5722");

    private static final String[] LANGUAGES = {"Bangla", "English", "Hindi", "Arabic", "Spanish", "French", "Urdu"};
    private static final String[] TONES = {"Polite", "Professional", "Friendly", "Casual", "Formal", "Direct", "Short", "Supportive"};
    private static final String[] STYLES = {"Professional", "Casual", "Friendly", "Formal", "Simple", "Academic"};

    // UI Components
    private final AiLayoutParams aiLayoutParams;
    private View aiBackButton;
    private ImageView selector_icon;
    private Spinner ai_selector_spinner;
    private View aiInitialView;
    private View selector_layout;
    private TextView btn_translate, btn_polish, btn_fix_grammar, btn_explain, btn_reply;
    private ProgressBar aiLoadingProgress;
    private RecyclerView aiResultsRecycler;
    private AiResultAdapter aiResultAdapter;

    // Managers and Listeners
    private AuthManager authManager;
    private ApiClient apiClient;
    private KeyboardActionListener keyboardActionListener = KeyboardActionListener.EMPTY_LISTENER;
    private OnAiActionListener onAiActionListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // State
    private boolean initialized = false;
    private boolean isSettingUpSpinner = false;
    private String selectedLanguage = DEFAULT_LANGUAGE;
    private String selectedTone = DEFAULT_TONE;
    private String selectedStyle = DEFAULT_STYLE;
    private String currentSelectorMode = "";
    private int selectorId;

    public AiView(final Context context) {
        this(context, null);
    }

    public AiView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.clipboardHistoryViewStyle);
    }

    public AiView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        aiLayoutParams = new AiLayoutParams(context.getResources());
    }

    public void setOnAiActionListener(final OnAiActionListener listener) {
        this.onAiActionListener = listener;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int width = helium314.keyboard.latin.utils.ResourceUtils.getKeyboardWidth(
            getContext(), helium314.keyboard.latin.settings.Settings.getValues())
            + getPaddingLeft() + getPaddingRight();
        final int height = aiLayoutParams.getTotalKeyboardHeight() + getPaddingTop() + getPaddingBottom();

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize() {
        if (initialized) return;

        // Initialize managers
        authManager = new AuthManager(getContext());
        apiClient = new ApiClient(getContext());

        // Set up 401 unauthorized listener
        apiClient.setUnauthorizedListener(() -> handler.post(() -> {
            Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getContext().startActivity(intent);
        }));

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Set up RecyclerView
        setupRecyclerView();

        initialized = true;
    }

    private void initializeViews() {
        aiBackButton = findViewById(R.id.ai_back_button);
        selector_icon = findViewById(R.id.selector_icon);
        ai_selector_spinner = findViewById(R.id.ai_selector_spinner);
        aiInitialView = findViewById(R.id.ai_initial_view);
        aiLoadingProgress = findViewById(R.id.ai_loading_progress);
        aiResultsRecycler = findViewById(R.id.ai_results_recycler);
        selector_layout = findViewById(R.id.selector_layout);
        btn_translate = findViewById(R.id.btn_translate);
        btn_polish = findViewById(R.id.btn_polish);
        btn_fix_grammar = findViewById(R.id.btn_fix_grammar);
        btn_explain = findViewById(R.id.btn_explain);
        btn_reply = findViewById(R.id.btn_reply);
    }

    private void setupClickListeners() {
        btn_translate.setOnClickListener(this);
        btn_polish.setOnClickListener(this);
        btn_fix_grammar.setOnClickListener(this);
        btn_explain.setOnClickListener(this);
        btn_reply.setOnClickListener(this);

        if (aiBackButton != null) {
            aiBackButton.setOnClickListener(v -> {
                if (keyboardActionListener != null) {
                    keyboardActionListener.onCodeInput(KeyCode.ALPHA,
                        helium314.keyboard.latin.common.Constants.NOT_A_COORDINATE,
                        helium314.keyboard.latin.common.Constants.NOT_A_COORDINATE, false);
                }
            });
        }
    }

    private void setupRecyclerView() {
        aiResultAdapter = new AiResultAdapter(result -> {
            if (onAiActionListener != null && !result.equals("No results found")) {
                onAiActionListener.onReplaceText(result);

                if (keyboardActionListener != null) {
                    keyboardActionListener.onCodeInput(KeyCode.ALPHA,
                        helium314.keyboard.latin.common.Constants.NOT_A_COORDINATE,
                        helium314.keyboard.latin.common.Constants.NOT_A_COORDINATE, false);
                }

            }
        });
        aiResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        aiResultsRecycler.setAdapter(aiResultAdapter);
    }

    private interface SpinnerSelectionListener {
        void onSelected(String value);
    }

    private void setupSpinner(Spinner spinner, String[] items, SpinnerSelectionListener listener) {
        if (spinner == null) return;

        isSettingUpSpinner = true;
        ArrayAdapter<String> adapter = createSpinnerAdapter(items);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSettingUpSpinner) {
                    listener.onSelected(items[position]);
                }
                isSettingUpSpinner = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    @NonNull
    private ArrayAdapter<String> createSpinnerAdapter(String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                R.layout.ai_spinner_item, android.R.id.text1, items);
        adapter.setDropDownViewResource(R.layout.ai_dropdown_item);
        return adapter;
    }

    private void showInitialView() {
        aiInitialView.setVisibility(View.VISIBLE);
        aiResultsRecycler.setVisibility(View.GONE);
        aiLoadingProgress.setVisibility(View.GONE);
    }

    private void showResults() {
        aiInitialView.setVisibility(View.GONE);
        aiResultsRecycler.setVisibility(View.VISIBLE);
        aiLoadingProgress.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        handler.post(() -> {
            aiLoadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                aiInitialView.setVisibility(View.GONE);
                aiResultsRecycler.setVisibility(View.GONE);
            }
        });
    }

    public void setHardwareAcceleratedDrawingEnabled(final boolean enabled) {
        if (!enabled) return;
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void startAiView(final KeyVisualAttributes keyVisualAttr, final EditorInfo editorInfo,
                           final KeyboardActionListener keyboardActionListener) {
        this.keyboardActionListener = keyboardActionListener;
        initialize();
        showInitialView();
        resetUi();

        final KeyDrawParams params = new KeyDrawParams();
        final Settings settings = Settings.getInstance();
        if (settings.getCustomTypeface() != null) {
            params.mTypeface = settings.getCustomTypeface();
        }
    }

    public void stopAiView() {
        if (!initialized) return;
        // Ensure RecyclerView is hidden when stopping the view
        aiResultsRecycler.setVisibility(View.GONE);
        // Cleanup if needed
    }

    private void resetUi() {
        selectedLanguage = DEFAULT_LANGUAGE;
        selectedTone = DEFAULT_TONE;
        selectedStyle = DEFAULT_STYLE;
        currentSelectorMode = "";
        selectorId = 0;

        resetButtonStates();
        updateSelector("");
        aiResultAdapter.reset();
    }

    private void resetButtonStates() {
        btn_translate.setSelected(false);
        btn_polish.setSelected(false);
        btn_fix_grammar.setSelected(false);
        btn_explain.setSelected(false);
        btn_reply.setSelected(false);
    }

    private void setChipSelected(TextView chip) {
        chip.setSelected(true);
    }

    @Override
    public void onClick(final View view) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
            KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS);

        if (!authManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Please login to Analysa Account", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            return;
        }

        selectorId = view.getId();
        handleAiFeature();
    }

    private void handleAiFeature() {
        if (selectorId == 0) return;

        aiResultAdapter.reset();
        resetButtonStates();

        if (selectorId == R.id.btn_translate) {
            handleTranslate();
        } else if (selectorId == R.id.btn_polish) {
            handlePolish();
        } else if (selectorId == R.id.btn_fix_grammar) {
            handleGrammarFix();
        } else if (selectorId == R.id.btn_explain) {
            handleExplain();
        } else if (selectorId == R.id.btn_reply) {
            handleReply();
        }
    }

    private void handleTranslate() {
        updateSelector("language");
        setChipSelected(btn_translate);
        String text = getClipboardText();
        if (!text.trim().isEmpty()) {
            translate(text, selectedLanguage);
        }
    }

    private void handlePolish() {
        updateSelector("style");
        setChipSelected(btn_polish);
        String text = getCurrentInputText();

        if (text.trim().isEmpty()) {
            Toast.makeText(getContext(), "Type or select some text first", Toast.LENGTH_SHORT).show();
            return;
        }
        performPolish(text, selectedStyle);
    }

    private void handleGrammarFix() {
        updateSelector("");
        setChipSelected(btn_fix_grammar);
        String text = getCurrentInputText();

        if (text.trim().isEmpty()) {
            Toast.makeText(getContext(), "Type or select some text first", Toast.LENGTH_SHORT).show();
            return;
        }
        performGrammarFix(text);
    }

    private void handleExplain() {
        updateSelector("language");
        setChipSelected(btn_explain);
        String text = getClipboardText();
        if (!text.trim().isEmpty()) {
            performExplain(text, selectedLanguage);
        }
    }

    private void handleReply() {
        updateSelector("tone");
        setChipSelected(btn_reply);
        String text = getClipboardText();
        if (!text.trim().isEmpty()) {
            Log.d(TAG, "Reply: " + text);
            performReply(text, selectedTone);
        }
    }

    private void updateSelector(String mode) {
        if (mode.isEmpty()) {
            currentSelectorMode = mode;
            selector_layout.setVisibility(GONE);
            return;
        }

        if (mode.equals(currentSelectorMode)) {
            return;
        }

        currentSelectorMode = mode;
        switch (mode) {
            case "language":
                selector_icon.setImageResource(R.drawable.translate);
                setupSpinner(ai_selector_spinner, LANGUAGES, lang -> {
                    selectedLanguage = lang;
                    handleAiFeature();
                });
                break;
            case "tone":
                selector_icon.setImageResource(R.drawable.tune_24dp);
                setupSpinner(ai_selector_spinner, TONES, tone -> {
                    selectedTone = tone;
                    handleAiFeature();
                });
                break;
            case "style":
                selector_icon.setImageResource(R.drawable.masked_transitions);
                setupSpinner(ai_selector_spinner, STYLES, style -> {
                    selectedStyle = style;
                    handleAiFeature();
                });
                break;
        }

        selector_layout.setVisibility(VISIBLE);
    }

    // API Call Methods
    private void performPolish(String text, String style) {
        showLoading(true);
        apiClient.polish(text, style, createApiCallback());
    }

    private void performExplain(String text, String lng) {
        showLoading(true);
        apiClient.explain(text, lng, createApiCallback());
    }

    private void performReply(String text, String tone) {
        showLoading(true);
        apiClient.reply(text, tone, createApiCallback());
    }

    private void performGrammarFix(String text) {
        showLoading(true);
        apiClient.grammarFix(text, createApiCallback());
    }

    private void translate(String text, String lng) {
        showLoading(true);
        apiClient.translate(text, lng, createApiCallback());
    }

    private ApiClient.ApiCallback createApiCallback() {
        return new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                handleAiResponse(response);
            }

            @Override
            public void onError(String message) {
                handleAiError(message);
            }
        };
    }

    private void handleAiResponse(JSONObject response) {
        handler.post(() -> {
            showLoading(false);
            List<String> results = parseResults(response);

            if (results.isEmpty()) {
                results.add("No results found");
            }

            showResults();
            aiResultAdapter.setResults(results);
        });
    }

    private List<String> parseResults(JSONObject response) {
        List<String> results = new ArrayList<>();
        Log.d(TAG, "Response: " + response.toString());

        try {
            if (response.has("result")) {
                Object data = response.get("result");
                if (data instanceof org.json.JSONArray) {
                    org.json.JSONArray array = (org.json.JSONArray) data;
                    for (int i = 0; i < array.length(); i++) {
                        results.add(array.getString(i));
                    }
                } else if (data instanceof String) {
                    results.add((String) data);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
        }

        return results;
    }

    private void handleAiError(String message) {
        handler.post(() -> {
            showLoading(false);
            Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
        });
    }

    private String getClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(getContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show();
            return "";
        }

        ClipDescription clipDescription = clipboard.getPrimaryClipDescription();
        if (clipDescription == null || !clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            Toast.makeText(getContext(), "Clipboard does not contain plain text.", Toast.LENGTH_SHORT).show();
            return "";
        }

        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        CharSequence text = item.getText();

        if (text != null) {
            return text.toString();
        }

        // Handle cases where getText() returns null (e.g., if it's a URI)
        String pasteData = item.coerceToText(getContext()).toString();
        return TextUtils.isEmpty(pasteData) ? "" : pasteData;
    }

    private String getCurrentInputText() {
        return onAiActionListener != null ? onAiActionListener.onGetCurrentText() : "";
    }

    public void setKeyboardActionListener(final KeyboardActionListener listener) {
        this.keyboardActionListener = listener;
    }
}

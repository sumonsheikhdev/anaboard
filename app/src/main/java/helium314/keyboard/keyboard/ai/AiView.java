// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.ai;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import helium314.keyboard.LoginActivity;
import helium314.keyboard.event.HapticEvent;
import helium314.keyboard.keyboard.KeyboardActionListener;
import helium314.keyboard.keyboard.internal.KeyDrawParams;
import helium314.keyboard.keyboard.internal.KeyVisualAttributes;
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode;
import helium314.keyboard.latin.AudioAndHapticFeedbackManager;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.settings.SettingsValues;
import helium314.keyboard.latin.utils.ResourceUtils;

@SuppressLint("CustomViewStyleable")
public class AiView extends LinearLayout implements View.OnClickListener {

    private final AiLayoutParams aiLayoutParams;
    private ScrollView aiActionsScrollView;
    private LinearLayout aiActionsContainer;
    private Button btn_login;
    private Button btnRephrase;
    private Button btnFixGrammar;
    private Button btnAddEmojis;
    private Button btnRealistic;
    private View aiBackButton;
    private Spinner aiLanguageSpinner;
    private ProgressBar aiLoadingProgress;
    private RecyclerView aiResultsRecycler;
    private AiResultAdapter aiResultAdapter;

    private KeyboardActionListener keyboardActionListener = KeyboardActionListener.EMPTY_LISTENER;
    private boolean initialized = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private OnAiActionListener onAiActionListener;

    private List<String> currentResults = Arrays.asList();
    private String selectedLanguage = "English";

    private final String[] languages = {"English", "Spanish", "French", "German", "Italian", "Portuguese", "Russian", "Chinese", "Japanese", "Korean"};

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
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = ResourceUtils.getKeyboardWidth(getContext(), Settings.getValues())
            + getPaddingLeft() + getPaddingRight();
        final int height = ResourceUtils.getKeyboardWidth(getContext(),
            Settings.getValues()) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize() {
        if (initialized) return;

        aiActionsScrollView = findViewById(R.id.ai_actions_scroll);
        aiActionsContainer = findViewById(R.id.ai_actions_container);
        btn_login = findViewById(R.id.btn_login);
        btnRephrase = findViewById(R.id.btn_rephrase);
        btnFixGrammar = findViewById(R.id.btn_fix_grammar);
        btnAddEmojis = findViewById(R.id.btn_add_emojis);
        btnRealistic = findViewById(R.id.btn_realistic);
        aiBackButton = findViewById(R.id.ai_back_button);
        aiLanguageSpinner = findViewById(R.id.ai_language_spinner);
        aiLoadingProgress = findViewById(R.id.ai_loading_progress);
        aiResultsRecycler = findViewById(R.id.ai_results_recycler);

        // Set up RecyclerView
        aiResultAdapter = new AiResultAdapter(result -> {
            if (keyboardActionListener != null) {
                keyboardActionListener.onTextInput(result);
            }
        });
        aiResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        aiResultsRecycler.setAdapter(aiResultAdapter);

        // Set up Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aiLanguageSpinner.setAdapter(adapter);
        aiLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.text_primary));
                String newLanguage = languages[position];
                if (!newLanguage.equals(selectedLanguage)) {
                    selectedLanguage = newLanguage;
                    if (!currentResults.isEmpty()) {
                        simulateApiCall(currentResults); // Simulate re-translating if we have results
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set click listeners
        if (btnRephrase != null) btnRephrase.setOnClickListener(this);
        if (btnFixGrammar != null) btnFixGrammar.setOnClickListener(this);
        if (btnAddEmojis != null) btnAddEmojis.setOnClickListener(this);
        if (btnRealistic != null) btnRealistic.setOnClickListener(this);

        if (aiBackButton != null) {
            aiBackButton.setOnClickListener(v -> {
                if (keyboardActionListener != null) {
                    keyboardActionListener.onCodeInput(KeyCode.ALPHA, helium314.keyboard.latin.common.Constants.NOT_A_COORDINATE, helium314.keyboard.latin.common.Constants.NOT_A_COORDINATE, false);
                }
            });
        }

        if (btn_login !=  null){
            btn_login.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                }
            });
        }

        initialized = true;
    }


    public void setHardwareAcceleratedDrawingEnabled(final boolean enabled) {
        if (!enabled) return;
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void startAiView(
        final KeyVisualAttributes keyVisualAttr,
        final EditorInfo editorInfo,
        final KeyboardActionListener keyboardActionListener) {
        this.keyboardActionListener = keyboardActionListener;
        initialize();

        final KeyDrawParams params = new KeyDrawParams();
        params.updateParams(aiLayoutParams.getBottomRowKeyboardHeight(), keyVisualAttr);
        final Settings settings = Settings.getInstance();
        if (settings.getCustomTypeface() != null) {
            params.mTypeface = settings.getCustomTypeface();
        }

        setupButtonStyles(params);
        setupSidePadding();
    }

    private void setupButtonStyles(final KeyDrawParams params) {
        // Apply consistent styling to buttons
        Button[] buttons = {btnRephrase, btnFixGrammar, btnAddEmojis, btnRealistic, btn_login};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setTypeface(params.mTypeface);
                btn.setTextColor(Color.WHITE);
//                btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, params.mLabelSize);
            }
        }
    }

    private void setupSidePadding() {
        final SettingsValues sv = Settings.getValues();
        final int keyboardWidth = ResourceUtils.getKeyboardWidth(getContext(), sv);
        final android.content.res.TypedArray keyboardAttr = getContext().obtainStyledAttributes(
            null, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard);
        final float leftPadding = keyboardAttr.getFraction(R.styleable.Keyboard_keyboardLeftPadding,
            keyboardWidth, keyboardWidth, 0f) * sv.mSidePaddingScale;
        final float rightPadding = keyboardAttr.getFraction(R.styleable.Keyboard_keyboardRightPadding,
            keyboardWidth, keyboardWidth, 0f) * sv.mSidePaddingScale;
        keyboardAttr.recycle();

        if (aiActionsScrollView != null) {
            aiActionsScrollView.setPadding(
                (int) leftPadding,
                aiActionsScrollView.getPaddingTop(),
                (int) rightPadding,
                aiActionsScrollView.getPaddingBottom()
            );
        }
    }

    public void stopAiView() {
        if (!initialized) return;
        // Cleanup if needed
    }

    @Override
    public void onClick(final View view) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
            KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS);

        final int id = view.getId();
        if (id == R.id.btn_rephrase) {
            simulateApiCall(Arrays.asList("Can you say that again?", "Could you repeat that?", "I didn't quite catch that.", "One more time, please?"));
        } else if (id == R.id.btn_fix_grammar) {
            simulateApiCall(Arrays.asList("It's correct now.", "Fixed the typos.", "Improved the flow.", "Grammar is perfect."));
        } else if (id == R.id.btn_add_emojis) {
            simulateApiCall(Arrays.asList("Hello! 👋", "Have a nice day! ☀️", "Looking forward to it! 😊", "Great job! 🚀"));
        } else if (id == R.id.btn_realistic) {
            simulateApiCall(Arrays.asList("Actually, it's quite simple.", "To be honest, I agree.", "In reality, it's different.", "Let's be real here."));
        }
    }

    private void simulateApiCall(List<String> baseResults) {
        currentResults = baseResults;
        aiLoadingProgress.setVisibility(View.VISIBLE);
        aiResultsRecycler.setVisibility(View.GONE);

        handler.postDelayed(() -> {
            aiLoadingProgress.setVisibility(View.GONE);
            aiResultsRecycler.setVisibility(View.VISIBLE);

            List<String> translatedResults = new ArrayList<>();
            for (String s : currentResults) {
                if (selectedLanguage.equals("English")) {
                    translatedResults.add(s);
                } else {
                    translatedResults.add("[" + selectedLanguage + "] " + s);
                }
            }
            aiResultAdapter.setResults(translatedResults);
        }, 1500); // 1.5 second delay
    }

    private String getCurrentInputText() {
        // This would ideally come from the input connection via onAiActionListener
        return "";
    }

    private void showToast(final String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void setKeyboardActionListener(final KeyboardActionListener listener) {
        this.keyboardActionListener = listener;
    }
}

package helium314.keyboard.keyboard.polish;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import helium314.keyboard.latin.R;

public class PolishPanelView extends LinearLayout {

    public PolishPanelView(Context context) {
        this(context, null);
    }

    public PolishPanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context)
                .inflate(R.layout.polish_panel, this, true);
    }
}

package helium314.keyboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import helium314.keyboard.latin.R;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private AuthManager authManager;
    private LinearLayout registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.loginProgressBar);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> performLogin());
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=dev.sumonsheikh.analysa"));
            startActivity(intent);
        });

    }

    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        setLoading(true);

        authManager.login(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(int id, String token) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    // Proceed to main screen or finish
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this,  message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager.getToken() != null) {
            // Proceed to main screen or finish
            finish();
        }
    }

}


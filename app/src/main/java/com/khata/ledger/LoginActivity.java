package com.khata.ledger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private TextView tvError, tvWorking;
    private Button btnSignIn, btnSignUp, btnGoogle;
    private FirebaseAuth auth;

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseGoogle(account.getIdToken());
                } catch (ApiException e) {
                    // 12501 = the person closed the Google dialog, not an error
                    if (e.getStatusCode() != 12501) {
                        showError(getString(R.string.googleFailed) + " (" + e.getStatusCode() + ")");
                    }
                }
            });

    /** default_web_client_id is generated from google-services.json. */
    public static GoogleSignInClient googleClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        tvWorking = findViewById(R.id.tvWorking);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoogle = findViewById(R.id.btnGoogle);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvError.setVisibility(View.GONE);
                googleLauncher.launch(googleClient(LoginActivity.this).getSignInIntent());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Already signed in? Go straight to the ledger.
        if (auth.getCurrentUser() != null) goMain();
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void busy(boolean on) {
        tvWorking.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private String friendly(Exception ex) {
        if (ex instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) ex).getErrorCode();
            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return getString(R.string.errInvalidEmail);
                case "ERROR_USER_NOT_FOUND":
                    return getString(R.string.errNoUser);
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                    return getString(R.string.errWrongCredential);
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return getString(R.string.errEmailInUse);
                case "ERROR_WEAK_PASSWORD":
                    return getString(R.string.errWeakPassword);
                default:
                    break;
            }
        }
        return ex == null || ex.getMessage() == null ? getString(R.string.errGeneric) : ex.getMessage();
    }

    private final OnCompleteListener<AuthResult> afterAuth = new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(Task<AuthResult> task) {
            busy(false);
            if (task.isSuccessful()) goMain();
            else showError(friendly(task.getException()));
        }
    };

    private void signIn() {
        tvError.setVisibility(View.GONE);
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.errEnterBoth));
            return;
        }
        busy(true);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(afterAuth);
    }

    private void signUp() {
        tvError.setVisibility(View.GONE);
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.errEnterBothCreate));
            return;
        }
        if (password.length() < 6) {
            showError(getString(R.string.errWeakPassword));
            return;
        }
        busy(true);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(afterAuth);
    }

    private void firebaseGoogle(String idToken) {
        busy(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(afterAuth);
    }
}

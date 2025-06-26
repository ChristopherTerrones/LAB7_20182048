package com.example.lab6_20182048;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.facebook.CallbackManager;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;


public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button loginBtn, facebookBtn;
    private TextView registerRedirect;
    private com.google.android.material.button.MaterialButton googleBtn;

    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;
    private CallbackManager facebookCallback;

    private final String TAG = "msg";

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleGoogleResult(result.getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initFirebase();
        initViews();
        configureGoogleLogin();
        configureFacebookLogin();

        loginBtn.setOnClickListener(v -> emailPasswordLogin());
        registerRedirect.setOnClickListener(v -> startActivity(new Intent(this, RegistroActivity.class)));
        googleBtn.setOnClickListener(v -> googleLauncher.launch(googleClient.getSignInIntent()));
        facebookBtn.setOnClickListener(v -> LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile")));
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        facebookCallback = CallbackManager.Factory.create();
    }

    private void initViews() {
        inputEmail = findViewById(R.id.campoCorreo);
        inputPassword = findViewById(R.id.campoClave);
        loginBtn = findViewById(R.id.botonEntrar);
        registerRedirect = findViewById(R.id.textoRegistro);
        googleBtn = findViewById(R.id.btnGoogle);
        facebookBtn = findViewById(R.id.btnFacebook);
    }

    private void configureGoogleLogin() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, options);
    }

    private void configureFacebookLogin() {
        LoginManager.getInstance().registerCallback(facebookCallback, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult result) {
                authenticateWithFacebook(result.getAccessToken());
            }

            @Override
            public void onCancel() {
                showToast("Inicio de sesión con Facebook cancelado");
            }

            @Override
            public void onError(FacebookException error) {
                showToast("Error al autenticar con Facebook");
            }
        });
    }

    private void emailPasswordLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Ingrese correo y contraseña");
            return;
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showToast("Inicio de sesión exitoso");
                redirectToDashboard();
            } else {
                showToast("Credenciales incorrectas");
            }
        });
    }

    private void handleGoogleResult(Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            if (account != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                signInWithCredential(credential, "Google");
            }
        } catch (ApiException e) {
            showToast("Fallo autenticación Google");
        }
    }

    private void authenticateWithFacebook(AccessToken token) {
        Log.d(TAG, "Facebook token: " + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        signInWithCredential(credential, "Facebook");
    }

    private void signInWithCredential(AuthCredential credential, String provider) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showToast("Inicio de sesión con " + provider);
                redirectToDashboard();
            } else {
                showToast("Fallo en autenticación con " + provider);
            }
        });
    }

    private void redirectToDashboard() {
        Intent i = new Intent(this, PanelPrincipalActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) redirectToDashboard();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookCallback.onActivityResult(requestCode, resultCode, data);
    }
}

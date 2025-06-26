package com.example.lab6_20182048;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class RegistroActivity extends AppCompatActivity {

    private EditText inputCorreo, inputClave, inputConfirmacion;
    private Button botonCrearCuenta;
    private TextView linkVolver;
    private FirebaseAuth autenticador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroLayout), (view, insets) -> {
            view.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        inicializarComponentes();
        configurarEventos();
    }

    private void inicializarComponentes() {
        autenticador = FirebaseAuth.getInstance();
        inputCorreo = findViewById(R.id.inputCorreo);
        inputClave = findViewById(R.id.inputClave);
        inputConfirmacion = findViewById(R.id.inputConfirmar);
        botonCrearCuenta = findViewById(R.id.btnCrearCuenta);
        linkVolver = findViewById(R.id.linkVolverLogin);
    }

    private void configurarEventos() {
        botonCrearCuenta.setOnClickListener(v -> procesarRegistro());
        linkVolver.setOnClickListener(v -> finish());
    }

    private void procesarRegistro() {
        String correo = inputCorreo.getText().toString().trim();
        String clave = inputClave.getText().toString().trim();
        String confirmacion = inputConfirmacion.getText().toString().trim();

        if (correo.isEmpty() || clave.isEmpty() || confirmacion.isEmpty()) {
            mostrarMensaje("Por favor, completa todos los campos");
        } else if (!clave.equals(confirmacion)) {
            mostrarMensaje("Las contraseñas no coinciden");
        } else if (clave.length() < 6) {
            mostrarMensaje("La contraseña debe tener al menos 6 caracteres");
        } else {
            registrarUsuario(correo, clave);
        }
    }

    private void registrarUsuario(String correo, String clave) {
        autenticador.createUserWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(this, tarea -> {
                    if (tarea.isSuccessful()) {
                        mostrarMensaje("Registro exitoso");
                        redirigirADashboard();
                    } else {
                        mostrarMensaje("Error al registrar. Intenta nuevamente.");
                    }
                });
    }

    private void redirigirADashboard() {
        Intent irDashboard = new Intent(this, PanelPrincipalActivity.class);
        irDashboard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(irDashboard);
        finish();
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
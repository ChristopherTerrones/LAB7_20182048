package com.example.lab6_20182048;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class PanelPrincipalActivity extends AppCompatActivity {

    private FirebaseAuth autenticacion;
    private BottomNavigationView barraInferior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel_principal);

        autenticacion = FirebaseAuth.getInstance();
        barraInferior = findViewById(R.id.navInferior);

        configurarBarraInferior();

        if (savedInstanceState == null) {
            mostrarFragmento(new IngresosFragment());
        }
    }

    private void configurarBarraInferior() {
        barraInferior.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menu) {
                return manejarSeleccion(menu.getItemId());
            }
        });
    }

    private boolean manejarSeleccion(int idOpcion) {
        if (idOpcion == R.id.nav_ingresos) {
            mostrarFragmento(new IngresosFragment());
        } else if (idOpcion == R.id.nav_egresos) {
            mostrarFragmento(new EgresosFragment());
        } else if (idOpcion == R.id.nav_resumen) {
            mostrarFragmento(new ResumenFinancieroFragment());
        } else if (idOpcion == R.id.nav_logout) {
            cerrarSesion();
            return true;
        } else {
            return false;
        }
        return true;
    }


    private void mostrarFragmento(Fragment fragmento) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedorFragmentos, fragmento)
                .commit();
    }

    private void cerrarSesion() {
        autenticacion.signOut();
        Toast.makeText(this, "Sesión finalizada correctamente", Toast.LENGTH_SHORT).show();
        Intent irLogin = new Intent(this, LoginActivity.class);
        irLogin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(irLogin);
        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Para salir, cierre sesión desde el menú", Toast.LENGTH_SHORT).show();
    }
}
package com.example.lab6_20182048;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class IngresosFragment extends Fragment implements MovimientosAdapter.ManejadorAcciones<Ingreso> {
    private RecyclerView listaIngresosView;
    private FloatingActionButton botonAgregarIngreso;
    private List<Ingreso> listaIngresos;
    private MovimientosAdapter<Ingreso> adaptador;
    private FirebaseFirestore db;
    private FirebaseUser usuario;
    private ActivityResultLauncher<String> selectorImagen;

    Button btnSeleccionar;
    ImageView imagePreview;
    Uri uriImagen = null;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        usuario = FirebaseAuth.getInstance().getCurrentUser();
        listaIngresos = new ArrayList<>();
        selectorImagen = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uriImagen = uri;
                        imagePreview.setImageURI(uri);
                    }
                }
        );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_ingresos, container, false);
        listaIngresosView = vista.findViewById(R.id.rvIngresos);
        listaIngresosView.setLayoutManager(new LinearLayoutManager(getContext()));
        adaptador = new MovimientosAdapter<>(getContext(), listaIngresos, true, this);
        listaIngresosView.setAdapter(adaptador);

        botonAgregarIngreso = vista.findViewById(R.id.fabAddIngreso);
        botonAgregarIngreso.setOnClickListener(v -> mostrarDialogoIngreso(null));

        if (usuario != null) cargarIngresos();
        else Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();

        return vista;
    }

    private void cargarIngresos() {
        db.collection("ingresos")
                .whereEqualTo("userId", usuario.getUid())
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    listaIngresos.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Ingreso ingreso = doc.toObject(Ingreso.class);
                        ingreso.setId(doc.getId());
                        listaIngresos.add(ingreso);
                    }
                    adaptador.actualizarLista(listaIngresos);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al cargar ingresos", Toast.LENGTH_SHORT).show());
    }

    private void mostrarDialogoIngreso(@Nullable Ingreso ingreso) {
        View vistaDialogo = LayoutInflater.from(getContext()).inflate(R.layout.dialog_movimiento, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setView(vistaDialogo);

        EditText titulo = vistaDialogo.findViewById(R.id.inputTituloIngreso);
        EditText monto = vistaDialogo.findViewById(R.id.inputMontoIngreso);
        EditText descripcion = vistaDialogo.findViewById(R.id.inputDescripcionIngreso);
        EditText fecha = vistaDialogo.findViewById(R.id.inputFechaIngreso);
        TextView tituloDialogo = vistaDialogo.findViewById(R.id.tituloFormulario);
        btnSeleccionar = vistaDialogo.findViewById(R.id.btnSeleccionarImagen);
        imagePreview = vistaDialogo.findViewById(R.id.imageComprobante);
        Button guardar = vistaDialogo.findViewById(R.id.btnGuardar);
        Button cancelar = vistaDialogo.findViewById(R.id.btnCancelar);
        uriImagen = null;

        AlertDialog dialogo = builder.create();

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (ingreso != null) {
            tituloDialogo.setText("Editar Ingreso");
            titulo.setText(ingreso.getTitulo());
            monto.setText(String.valueOf(ingreso.getMonto()));
            descripcion.setText(ingreso.getDescripcion());
            if (ingreso.getFecha() != null) fecha.setText(formatoFecha.format(ingreso.getFecha()));
            titulo.setEnabled(false);
            fecha.setEnabled(false);
            if (ingreso.getComprobanteUrl() != null) {
                ServicioAlmacenamiento.visualizarArchivo(getContext(), ingreso.getComprobanteUrl(), imagePreview);
            }
        } else {
            tituloDialogo.setText("Nuevo Ingreso");
            fecha.setText(formatoFecha.format(new Date()));
        }

        fecha.setOnClickListener(v -> mostrarDatePicker(fecha));
        btnSeleccionar.setOnClickListener(v -> selectorImagen.launch("image/*"));

        guardar.setOnClickListener(v -> {
            String tituloVal = titulo.getText().toString().trim();
            String montoVal = monto.getText().toString().trim();
            String desc = descripcion.getText().toString().trim();
            String fechaVal = fecha.getText().toString().trim();

            if (tituloVal.isEmpty() || montoVal.isEmpty() || fechaVal.isEmpty()) {
                Toast.makeText(getContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            double montoNum;
            try {
                montoNum = Double.parseDouble(montoVal);
                if (montoNum <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                Toast.makeText(getContext(), "Monto inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            Date fechaFinal;
            try {
                fechaFinal = formatoFecha.parse(fechaVal);
            } catch (Exception ex) {
                Toast.makeText(getContext(), "Fecha inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            if (usuario == null) return;

            if (uriImagen == null && ingreso == null) {
                Toast.makeText(getContext(), "Debe seleccionar un comprobante", Toast.LENGTH_SHORT).show();
                return;
            }

            if (uriImagen != null) {
                ServicioAlmacenamiento.guardarArchivo(getContext(), uriImagen, new ServicioAlmacenamiento.CallbackSubida() {
                    @Override
                    public void exito(String url) {
                        guardarIngresoEnFirestore(tituloVal, montoNum, desc, fechaFinal, url, dialogo, ingreso);
                    }

                    @Override
                    public void error(String mensaje) {
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                guardarIngresoEnFirestore(tituloVal, montoNum, desc, fechaFinal, ingreso.getComprobanteUrl(), dialogo, ingreso);
            }
        });

        cancelar.setOnClickListener(v -> dialogo.dismiss());
        dialogo.show();
    }

    private void mostrarDatePicker(EditText campoFecha) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            Calendar seleccionado = Calendar.getInstance();
            seleccionado.set(year, month, dayOfMonth);
            campoFecha.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(seleccionado.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void editarMovimiento(String id, Ingreso ingreso) {
        mostrarDialogoIngreso(ingreso);
    }

    @Override
    public void eliminarMovimiento(String id, Ingreso ingreso) {
        db.collection("ingresos").document(id).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Ingreso eliminado", Toast.LENGTH_SHORT).show();
                    cargarIngresos();
                });
    }
    private void guardarIngresoEnFirestore(String titulo, double monto, String descripcion, Date fecha, String url, AlertDialog dialogo, @Nullable Ingreso ingresoExistente) {
        Ingreso actualizado = new Ingreso(usuario.getUid(), titulo, monto, descripcion, fecha, url);

        if (ingresoExistente != null && ingresoExistente.getId() != null) {
            db.collection("ingresos").document(ingresoExistente.getId())
                    .set(actualizado)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(getContext(), "Ingreso actualizado", Toast.LENGTH_SHORT).show();
                        dialogo.dismiss();
                        cargarIngresos();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("ingresos")
                    .add(actualizado)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(getContext(), "Ingreso registrado", Toast.LENGTH_SHORT).show();
                        dialogo.dismiss();
                        cargarIngresos();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());
        }
    }

}
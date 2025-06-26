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

public class EgresosFragment extends Fragment implements MovimientosAdapter.ManejadorAcciones<Egreso> {

    private RecyclerView recyclerEgresos;
    private FloatingActionButton botonAgregarEgreso;
    private MovimientosAdapter<Egreso> adaptadorEgresos;
    private List<Egreso> listaEgresos;
    private FirebaseFirestore firestore;
    private FirebaseUser usuario;

    private ActivityResultLauncher<String> selectorImagen;
    private Uri uriImagen = null;
    private ImageView imagePreview;

    public EgresosFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        usuario = FirebaseAuth.getInstance().getCurrentUser();
        listaEgresos = new ArrayList<>();

        selectorImagen = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uriImagen = uri;
                        if (imagePreview != null) {
                            imagePreview.setImageURI(uri);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_egresos, container, false);

        recyclerEgresos = vista.findViewById(R.id.listadoEgresos);
        botonAgregarEgreso = vista.findViewById(R.id.fabAddEgreso);
        recyclerEgresos.setLayoutManager(new LinearLayoutManager(getContext()));

        adaptadorEgresos = new MovimientosAdapter<>(getContext(), listaEgresos, false, this);
        recyclerEgresos.setAdapter(adaptadorEgresos);

        botonAgregarEgreso.setOnClickListener(v -> mostrarDialogoEgreso(null));

        if (usuario != null) {
            obtenerEgresos();
        } else {
            Toast.makeText(getContext(), "No se encontró usuario", Toast.LENGTH_SHORT).show();
        }

        return vista;
    }

    private void obtenerEgresos() {
        firestore.collection("egresos")
                .whereEqualTo("userId", usuario.getUid())
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    listaEgresos.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Egreso egreso = doc.toObject(Egreso.class);
                        egreso.setId(doc.getId());
                        listaEgresos.add(egreso);
                    }
                    adaptadorEgresos.actualizarLista(listaEgresos);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al cargar egresos", Toast.LENGTH_SHORT).show());
    }

    private void mostrarDialogoEgreso(@Nullable Egreso egreso) {
        View vista = LayoutInflater.from(getContext()).inflate(R.layout.dialog_movimiento, null);
        AlertDialog dialogo = new AlertDialog.Builder(getContext()).setView(vista).create();

        EditText campoTitulo = vista.findViewById(R.id.inputTituloIngreso);
        EditText campoMonto = vista.findViewById(R.id.inputMontoIngreso);
        EditText campoDescripcion = vista.findViewById(R.id.inputDescripcionIngreso);
        EditText campoFecha = vista.findViewById(R.id.inputFechaIngreso);
        TextView tituloDialogo = vista.findViewById(R.id.tituloFormulario);
        Button botonGuardar = vista.findViewById(R.id.btnGuardar);
        Button botonCancelar = vista.findViewById(R.id.btnCancelar);
        Button btnSeleccionar = vista.findViewById(R.id.btnSeleccionarImagen);
        imagePreview = vista.findViewById(R.id.imageComprobante);
        uriImagen = null;

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (egreso != null) {
            tituloDialogo.setText("Editar Egreso");
            campoTitulo.setText(egreso.getTitulo());
            campoMonto.setText(String.valueOf(egreso.getMonto()));
            campoDescripcion.setText(egreso.getDescripcion());
            if (egreso.getFecha() != null) campoFecha.setText(formatoFecha.format(egreso.getFecha()));
            campoTitulo.setEnabled(false);
            campoFecha.setEnabled(false);
            if (egreso.getComprobanteUrl() != null) {
                ServicioAlmacenamiento.visualizarArchivo(getContext(), egreso.getComprobanteUrl(), imagePreview);
            }
        } else {
            tituloDialogo.setText("Nuevo Egreso");
            campoFecha.setText(formatoFecha.format(Calendar.getInstance().getTime()));
        }

        campoFecha.setOnClickListener(v -> mostrarSelectorFecha(campoFecha, formatoFecha));
        btnSeleccionar.setOnClickListener(v -> selectorImagen.launch("image/*"));

        botonGuardar.setOnClickListener(v -> {
            String titulo = campoTitulo.getText().toString().trim();
            String montoTexto = campoMonto.getText().toString().trim();
            String descripcion = campoDescripcion.getText().toString().trim();
            String fechaTexto = campoFecha.getText().toString().trim();

            if (titulo.isEmpty() || montoTexto.isEmpty() || fechaTexto.isEmpty()) {
                Toast.makeText(getContext(), "Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            double monto;
            try {
                monto = Double.parseDouble(montoTexto);
                if (monto <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Monto inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            Date fecha;
            try {
                fecha = formatoFecha.parse(fechaTexto);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Fecha inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            if (usuario == null) return;

            if (uriImagen == null && egreso == null) {
                Toast.makeText(getContext(), "Debe seleccionar un comprobante", Toast.LENGTH_SHORT).show();
                return;
            }

            if (uriImagen != null) {
                ServicioAlmacenamiento.guardarArchivo(getContext(), uriImagen, new ServicioAlmacenamiento.CallbackSubida() {
                    @Override
                    public void exito(String url) {
                        guardarEgresoFirestore(titulo, monto, descripcion, fecha, url, dialogo, egreso);

                    }

                    @Override
                    public void error(String mensaje) {
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                guardarEgresoFirestore(titulo, monto, descripcion, fecha, egreso.getComprobanteUrl(), dialogo, egreso);
            }
        });

        botonCancelar.setOnClickListener(v -> dialogo.dismiss());
        dialogo.show();
    }

    private void mostrarSelectorFecha(EditText campoFecha, SimpleDateFormat formato) {
        Calendar calendario = Calendar.getInstance();
        try {
            Date fecha = formato.parse(campoFecha.getText().toString());
            if (fecha != null) calendario.setTime(fecha);
        } catch (Exception ignored) {}

        new DatePickerDialog(getContext(), (view, y, m, d) -> {
            calendario.set(y, m, d);
            campoFecha.setText(formato.format(calendario.getTime()));
        }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void guardarEgresoFirestore(String titulo, double monto, String descripcion, Date fecha, String url, AlertDialog dialogo, @Nullable Egreso egresoExistente) {
        Egreso actualizado = new Egreso(usuario.getUid(), titulo, monto, descripcion, fecha, url);

        if (egresoExistente != null && egresoExistente.getId() != null) {
            firestore.collection("egresos").document(egresoExistente.getId())
                    .set(actualizado)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(getContext(), "Egreso actualizado", Toast.LENGTH_SHORT).show();
                        dialogo.dismiss();
                        obtenerEgresos();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show());
        } else {
            firestore.collection("egresos")
                    .add(actualizado)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(getContext(), "Egreso guardado", Toast.LENGTH_SHORT).show();
                        dialogo.dismiss();
                        obtenerEgresos();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());
        }
    }


    @Override
    public void editarMovimiento(String id, Egreso egreso) {
        mostrarDialogoEgreso(egreso);
    }

    @Override
    public void eliminarMovimiento(String id, Egreso egreso) {
        firestore.collection("egresos").document(id).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Egreso eliminado", Toast.LENGTH_SHORT).show();
                    obtenerEgresos();
                });
    }
}
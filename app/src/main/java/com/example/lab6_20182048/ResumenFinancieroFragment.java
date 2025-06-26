package com.example.lab6_20182048;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ResumenFinancieroFragment extends Fragment implements DatePickerDialog.OnDateSetListener {
    private GraficoPastelFinanzasView vistaPastel;
    private GraficoBarrasFinanzasView vistaBarras;
    private TextView textoMesSeleccionado;
    private ImageButton botonMesAnterior, botonMesSiguiente;
    private FirebaseFirestore baseDatos;
    private FirebaseAuth autenticacion;
    private FirebaseUser usuarioActual;
    private Calendar calendarioMesActual;

    public ResumenFinancieroFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseDatos = FirebaseFirestore.getInstance();
        autenticacion = FirebaseAuth.getInstance();
        usuarioActual = autenticacion.getCurrentUser();
        calendarioMesActual = Calendar.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_resumen, container, false);

        vistaPastel = vista.findViewById(R.id.pieChart);
        vistaBarras = vista.findViewById(R.id.barChart);
        textoMesSeleccionado = vista.findViewById(R.id.textViewSelectedMonth);
        botonMesAnterior = vista.findViewById(R.id.buttonPreviousMonth);
        botonMesSiguiente = vista.findViewById(R.id.buttonNextMonth);

        configurarSelectorMes();
        cargarDatosGraficos();

        return vista;
    }

    private void configurarSelectorMes() {
        actualizarVisualMes();
        textoMesSeleccionado.setOnClickListener(v -> {
            DatePickerDialog selectorFecha = DatePickerDialog.newInstance(
                    this,
                    calendarioMesActual.get(Calendar.YEAR),
                    calendarioMesActual.get(Calendar.MONTH),
                    calendarioMesActual.get(Calendar.DAY_OF_MONTH)
            );
            selectorFecha.show(getParentFragmentManager(), "SelectorFecha");
        });

        botonMesAnterior.setOnClickListener(v -> {
            calendarioMesActual.add(Calendar.MONTH, -1);
            actualizarVisualMes();
            cargarDatosGraficos();
        });

        botonMesSiguiente.setOnClickListener(v -> {
            calendarioMesActual.add(Calendar.MONTH, 1);
            actualizarVisualMes();
            cargarDatosGraficos();
        });
    }

    private void actualizarVisualMes() {
        SimpleDateFormat formatoMes = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));
        String mesFormateado = formatoMes.format(calendarioMesActual.getTime());
        mesFormateado = mesFormateado.substring(0,1).toUpperCase() + mesFormateado.substring(1);
        textoMesSeleccionado.setText(mesFormateado);
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        calendarioMesActual.set(Calendar.YEAR, year);
        calendarioMesActual.set(Calendar.MONTH, monthOfYear);
        calendarioMesActual.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        actualizarVisualMes();
        cargarDatosGraficos();
    }

    private void cargarDatosGraficos() {
        if (usuarioActual == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar inicioMes = (Calendar) calendarioMesActual.clone();
        inicioMes.set(Calendar.DAY_OF_MONTH, 1);
        inicioMes.set(Calendar.HOUR_OF_DAY, 0);
        inicioMes.set(Calendar.MINUTE, 0);
        inicioMes.set(Calendar.SECOND, 0);
        inicioMes.set(Calendar.MILLISECOND, 0);

        Calendar finMes = (Calendar) calendarioMesActual.clone();
        finMes.set(Calendar.DAY_OF_MONTH, finMes.getActualMaximum(Calendar.DAY_OF_MONTH));
        finMes.set(Calendar.HOUR_OF_DAY, 23);
        finMes.set(Calendar.MINUTE, 59);
        finMes.set(Calendar.SECOND, 59);
        finMes.set(Calendar.MILLISECOND, 999);

        Date fechaInicio = inicioMes.getTime();
        Date fechaFin = finMes.getTime();

        Task<QuerySnapshot> consultaIngresos = baseDatos.collection("ingresos")
                .whereEqualTo("userId", usuarioActual.getUid())
                .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                .whereLessThanOrEqualTo("fecha", fechaFin)
                .get();

        Task<QuerySnapshot> consultaEgresos = baseDatos.collection("egresos")
                .whereEqualTo("userId", usuarioActual.getUid())
                .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                .whereLessThanOrEqualTo("fecha", fechaFin)
                .get();

        Tasks.whenAllSuccess(consultaIngresos, consultaEgresos).addOnSuccessListener(resultados -> {
            double sumaIngresos = 0;
            for (QueryDocumentSnapshot doc : (QuerySnapshot) resultados.get(0)) {
                Ingreso ingreso = doc.toObject(Ingreso.class);
                sumaIngresos += ingreso.getMonto();
            }
            double sumaEgresos = 0;
            for (QueryDocumentSnapshot doc : (QuerySnapshot) resultados.get(1)) {
                Egreso egreso = doc.toObject(Egreso.class);
                sumaEgresos += egreso.getMonto();
            }
            actualizarPantalla(sumaIngresos, sumaEgresos);
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error al obtener datos", Toast.LENGTH_SHORT).show();
        });
    }

    private void actualizarPantalla(double ingresos, double egresos) {
        vistaPastel.establecerDatos((float) ingresos, (float) egresos);
        vistaBarras.establecerDatos(ingresos, egresos);
    }
}

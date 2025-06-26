package com.example.lab6_20182048;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MovimientosAdapter<T> extends RecyclerView.Adapter<MovimientosAdapter.MovimientoViewHolder> {
    private Context contexto;
    private List<T> listaMovimientos;
    private boolean esIngreso;
    private ManejadorAcciones<T> manejador;

    public MovimientosAdapter(Context contexto, List<T> listaMovimientos, boolean esIngreso, ManejadorAcciones<T> manejador) {
        this.contexto = contexto;
        this.listaMovimientos = listaMovimientos;
        this.esIngreso = esIngreso;
        this.manejador = manejador;
    }

    public void actualizarLista(List<T> nuevosDatos) {
        this.listaMovimientos = nuevosDatos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovimientoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_ingreso_egreso, parent, false);
        return new MovimientoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull MovimientoViewHolder holder, int posicion) {
        T movimiento = listaMovimientos.get(posicion);

        String titulo;
        double monto = 0;
        String descripcion = "";
        String id;
        java.util.Date fecha = null;
        String comprobanteUrl;

        if (esIngreso) {
            Ingreso ingreso = (Ingreso) movimiento;
            titulo = ingreso.getTitulo();
            monto = ingreso.getMonto();
            descripcion = ingreso.getDescripcion();
            fecha = ingreso.getFecha();
            id = ingreso.getId();
            comprobanteUrl = ingreso.getComprobanteUrl();
        } else {
            Egreso egreso = (Egreso) movimiento;
            titulo = egreso.getTitulo();
            monto = egreso.getMonto();
            descripcion = egreso.getDescripcion();
            fecha = egreso.getFecha();
            id = egreso.getId();
            comprobanteUrl = egreso.getComprobanteUrl();
        }

        holder.titulo.setText(titulo);
        holder.descripcion.setText(descripcion);

        String montoTexto = String.format(Locale.getDefault(), "S/. %.2f", monto);
        holder.monto.setText((esIngreso ? "+ " : "- ") + montoTexto);
        holder.monto.setTextColor(contexto.getResources().getColor(
                esIngreso ? android.R.color.holo_blue_dark : android.R.color.holo_red_dark));

        if (fecha != null) {
            String fechaFormateada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha);
            holder.fecha.setText(fechaFormateada);
        } else {
            holder.fecha.setText("Fecha no disponible");
        }

        holder.btnEditar.setOnClickListener(v -> {
            if (manejador != null) manejador.editarMovimiento(id, movimiento);
        });

        holder.btnEliminar.setOnClickListener(v -> mostrarDialogoEliminacion(id, movimiento));
        holder.btnDescargar.setOnClickListener(v -> {
            if (comprobanteUrl != null) {
                ServicioAlmacenamiento.obtenerArchivo(contexto, comprobanteUrl, "comprobante_" + titulo + ".jpg");
            } else {
                Toast.makeText(contexto, "No hay comprobante", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void mostrarDialogoEliminacion(String id, T movimiento) {
        new AlertDialog.Builder(contexto)
                .setTitle("Eliminar registro")
                .setMessage("¿Deseas eliminar este elemento?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    if (manejador != null) manejador.eliminarMovimiento(id, movimiento);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return listaMovimientos.size();
    }

    public static class MovimientoViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, monto, fecha, descripcion;
        ImageButton btnEditar, btnEliminar, btnDescargar;

        public MovimientoViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.tituloOperacion);
            monto = itemView.findViewById(R.id.montoOperacion);
            fecha = itemView.findViewById(R.id.fechaOperacion);
            descripcion = itemView.findViewById(R.id.detalleOperacion);
            btnEditar = itemView.findViewById(R.id.iconoEditar);
            btnEliminar = itemView.findViewById(R.id.iconoEliminar);
            btnDescargar = itemView.findViewById(R.id.btnDescargar);
        }
    }

    public interface ManejadorAcciones<T> {
        void editarMovimiento(String id, T movimiento);
        void eliminarMovimiento(String id, T movimiento);
    }
}

package com.example.lab6_20182048;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GraficoBarrasFinanzasView extends View {
    private Paint paintBarra;
    private Paint paintEtiqueta;
    private float valorIngresos = 0;
    private float valorEgresos = 0;
    private float valorConsolidado = 0;
    private float valorMaximo = 100;

    public GraficoBarrasFinanzasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    private void inicializar() {
        paintBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarra.setStyle(Paint.Style.FILL);

        paintEtiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEtiqueta.setColor(Color.DKGRAY);
        paintEtiqueta.setTextSize(32f);
        paintEtiqueta.setTextAlign(Paint.Align.CENTER);
    }

    public void establecerDatos(double ingresos, double egresos) {
        valorIngresos = (float) ingresos;
        valorEgresos = (float) egresos;
        valorConsolidado = valorIngresos - valorEgresos;
        valorMaximo = Math.max(Math.max(valorIngresos, valorEgresos), Math.abs(valorConsolidado));
        if (valorMaximo == 0) valorMaximo = 100;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int ancho = getWidth();
        int alto = getHeight();
        float anchoBarra = ancho / 7f;
        float espacio = anchoBarra * 0.8f;
        float margenInferior = 60f;
        float margenSuperior = 40f;
        float alturaUtil = alto - margenSuperior - margenInferior;

        float[] valores = {valorIngresos, valorEgresos, Math.abs(valorConsolidado)};
        int[] colores = {
                Color.parseColor("#2196F3"),
                Color.parseColor("#F44336"),
                Color.BLACK
        };

        String[] etiquetas = {"Ingresos", "Egresos", "Consolidado"};

        for (int i = 0; i < valores.length; i++) {
            float alturaBarra = (valores[i] / valorMaximo) * alturaUtil;
            float xInicio = espacio + i * (anchoBarra + espacio);
            float yInicio = alto - margenInferior - alturaBarra;
            float yFin = alto - margenInferior;

            paintBarra.setColor(colores[i]);
            canvas.drawRect(xInicio, yInicio, xInicio + anchoBarra, yFin, paintBarra);
            canvas.drawText(etiquetas[i], xInicio + anchoBarra / 2, alto - 20, paintEtiqueta);
        }
    }
}

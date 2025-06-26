package com.example.lab6_20182048;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraficoPastelFinanzasView extends View {
    private Paint paintSector;
    private RectF circulo;
    private final List<Sector> sectores = new ArrayList<>();

    public GraficoPastelFinanzasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    private void inicializar() {
        paintSector = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSector.setStyle(Paint.Style.FILL);
    }

    public void establecerDatos(float ingresos, float egresos) {
        sectores.clear();
        float total = ingresos + egresos;
        if (total <= 0) {
            invalidate();
            return;
        }

        float anguloIngresos = (ingresos / total) * 360f;
        float anguloEgresos = (egresos / total) * 360f;

        if (ingresos > 0) sectores.add(new Sector(anguloIngresos, Color.parseColor("#2196F3")));
        if (egresos > 0) sectores.add(new Sector(anguloEgresos, Color.parseColor("#F44336")));

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (sectores.isEmpty()) {
            paintSector.setColor(Color.LTGRAY);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 4f, paintSector);
            paintSector.setColor(Color.DKGRAY);
            paintSector.setTextSize(30f);
            paintSector.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Sin datos", getWidth() / 2f, getHeight() / 2f, paintSector);
            return;
        }

        if (circulo == null) {
            float dimension = Math.min(getWidth(), getHeight()) * 0.85f;
            float izquierdo = (getWidth() - dimension) / 2f;
            float superior = (getHeight() - dimension) / 2f;
            circulo = new RectF(izquierdo, superior, izquierdo + dimension, superior + dimension);
        }

        float inicio = 0;
        for (Sector s : sectores) {
            paintSector.setColor(s.color);
            canvas.drawArc(circulo, inicio, s.angulo, true, paintSector);
            inicio += s.angulo;
        }
    }

    private static class Sector {
        float angulo;
        int color;

        Sector(float angulo, int color) {
            this.angulo = angulo;
            this.color = color;
        }
    }
}


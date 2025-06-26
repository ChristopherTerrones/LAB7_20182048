package com.example.lab6_20182048;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServicioAlmacenamiento {

    private static boolean isInitialized = false;

    // Método 1: Conexión
    public static void conectarServicio(Context context) {
        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dssoxggz3");
            config.put("api_key", "996445227885663");
            config.put("api_secret", "2WqO6YaSuZyO0WXc8cxj4JkArxU");
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }

    // Método 2: Guardar archivo en la nube
    public static void guardarArchivo(Context context, Uri uri, CallbackSubida callback) {
        conectarServicio(context);
        String publicId = "comprobante_" + UUID.randomUUID().toString();

        MediaManager.get().upload(uri)
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = resultData.get("secure_url").toString();
                        callback.exito(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.error("Error al subir: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        callback.error("Reintento fallido: " + error.getDescription());
                    }
                }).dispatch();
    }

    // Método extra: Visualizar archivo
    public static void visualizarArchivo(Context context, String url, ImageView destino) {
        Glide.with(context).load(url).into(destino);
    }

    // Método 3: Obtener archivo
    public static void obtenerArchivo(Context context, String url, String nombreArchivo) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Descargando comprobante");
        request.setDescription(nombreArchivo);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombreArchivo);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    public interface CallbackSubida {
        void exito(String url);
        void error(String mensaje);
    }
}


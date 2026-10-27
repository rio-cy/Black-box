package top.niunaijun.blackbox.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;

public class LogSender {
    private static final String TAG = "LogSender";

    public static String send(String chatId, File logFile, String caption) {
        if (logFile == null || !logFile.exists()) {
            Slog.w(TAG, "Log file not found: " + logFile);
            return "Log file not found";
        }

        try {
            File storageDir = null;
            if (BlackBoxCore.getContext() != null) {
                storageDir = BlackBoxCore.getContext().getExternalFilesDir("crash_logs");
                if (storageDir == null) {
                    storageDir = new File(BlackBoxCore.getContext().getFilesDir(), "crash_logs");
                }
            } else {
                storageDir = new File(logFile.getParentFile(), "crash_logs");
            }

            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File savedFile = new File(storageDir, "crash_log_" + timestamp + ".txt");

            try (FileInputStream fis = new FileInputStream(logFile);
                 FileOutputStream fos = new FileOutputStream(savedFile)) {
                
                if (caption != null && !caption.isEmpty()) {
                    String header = "Caption: " + caption + "\nSaved: " + new Date().toString() + "\n\n";
                    fos.write(header.getBytes("UTF-8"));
                }

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
            }

            Slog.d(TAG, "Log successfully saved locally to: " + savedFile.getAbsolutePath());
            return null; // Null indicates success
        } catch (Exception e) {
            Slog.e(TAG, "Error saving crash log locally: " + e.getMessage(), e);
            return "Local Save Exception: " + e.getMessage();
        }
    }
}

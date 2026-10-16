package com.example.amazonMages.dev;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Debug;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/** Low-frequency, best-effort Android diagnostics; unavailable sensors are never fabricated. */
public final class DevicePerformanceMonitor {
    private final Context context;
    private final ActivityManager activity;
    private final PowerManager power;
    private long lastSampleMs;
    private long lastCpuTicks = -1;
    private long lastWallMs;
    private String snapshot = "Aguardando amostragem...";

    public DevicePerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        activity = (ActivityManager) this.context.getSystemService(Context.ACTIVITY_SERVICE);
        power = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
    }

    public String snapshot() { return snapshot; }

    public void sampleIfDue() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastSampleMs < 1000) return;
        lastSampleMs = now;
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();
        long nativeHeap = Debug.getNativeHeapAllocatedSize();
        StringBuilder result = new StringBuilder();
        result.append(String.format(Locale.US, "Heap: %.1f/%.1f MB | Native: %.1f MB",
            mb(heapUsed), mb(heapMax), mb(nativeHeap)));
        if (activity != null) {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            activity.getMemoryInfo(info);
            result.append(String.format(Locale.US, "\nRAM livre: %.0f MB | baixa: %s",
                mb(info.availMem), info.lowMemory ? "sim" : "nao"));
        }
        result.append("\nCPU nucleos: ").append(Runtime.getRuntime().availableProcessors());
        long cpuTicks = readProcessCpuTicks();
        if (cpuTicks >= 0 && lastCpuTicks >= 0 && now > lastWallMs) {
            // Linux USER_HZ is commonly 100, but not guaranteed: report tick rate rather than fake %.
            result.append(String.format(Locale.US, " | proc: %d ticks/s",
                (cpuTicks - lastCpuTicks) * 1000 / (now - lastWallMs)));
        }
        lastCpuTicks = cpuTicks;
        lastWallMs = now;
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) result.append("\nBateria: ").append(level * 100 / scale).append("%");
            int temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            if (temperature != Integer.MIN_VALUE)
                result.append(String.format(Locale.US, " | %.1f C (bateria)", temperature / 10.0));
        }
        if (Build.VERSION.SDK_INT >= 29 && power != null) {
            int status = power.getCurrentThermalStatus();
            result.append("\nEstado termico: ").append(thermalLabel(status));
        } else result.append("\nEstado termico: indisponivel");
        snapshot = result.toString();
    }

    private static double mb(long bytes) { return bytes / 1048576.0; }

    private static String thermalLabel(int value) {
        switch (value) {
            case PowerManager.THERMAL_STATUS_NONE: return "normal";
            case PowerManager.THERMAL_STATUS_LIGHT: return "leve";
            case PowerManager.THERMAL_STATUS_MODERATE: return "moderado";
            case PowerManager.THERMAL_STATUS_SEVERE: return "severo";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "critico";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "emergencia";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "desligamento iminente";
            default: return "desconhecido";
        }
    }

    private static long readProcessCpuTicks() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/stat"))) {
            String stat = reader.readLine();
            if (stat == null) return -1;
            int end = stat.lastIndexOf(')');
            if (end < 0) return -1;
            String[] fields = stat.substring(end + 2).split("\\s+");
            // Field 3 is fields[0], utime/stime are fields[11]/[12].
            return Long.parseLong(fields[11]) + Long.parseLong(fields[12]);
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }
}

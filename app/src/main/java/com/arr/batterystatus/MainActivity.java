package com.arr.batterystatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.arr.batterystatus.databinding.ActivityMainBinding;
import com.arr.batterystatus.utils.DialogDescription;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private BroadcastReceiver batteryReceiver;
    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        getBatteryCapacity();

        runnable =
                new Runnable() {
                    @Override
                    public void run() {
                        updatePowerDisplay();
                        // Vuelve a ejecutar este Runnable en 1 segundo
                        handler.postDelayed(this, 1000);
                    }
                };

        binding.mainContent.cardWatts.setOnClickListener(this::setMessageWatts);
        
    }

    private void setMessageWatts(View view) {
        new DialogDescription(this).setMessage(R.string.watts_description).show();
    }

    private void updatePowerDisplay() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);

        // Obtener el voltaje de la batería en milivoltios (mV)
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        calculatePorcentage(intent);
        statusCharging(intent);
        infoBattery(intent, batteryManager);
    }

    private void calculatePorcentage(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        int batteryPct = (int) ((level / (float) scale) * 100);
        binding.mainContent.battery.setText(batteryPct + "%");
        binding.mainContent.chargerPorcent.setProgress(batteryPct);
    }

    // estado de carga
    private void statusCharging(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL;
        binding.mainContent.charginStatus.setText(isCharging ? "Cargando" : "Desconectado");
    }

    private void infoBattery(Intent intent, BatteryManager manager) {

        // obtener voltaje
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        float volt = voltage / 1000f;
        binding.mainContent.textVoltage.setText(volt + " V");

        int currentMicroAmps = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        // obtener amperios
        float currentMilliAmps = currentMicroAmps / 1000f;
        if (currentMilliAmps != Integer.MIN_VALUE) {
            binding.mainContent.textAmperios.setText(
                    String.format("%.2f mA", Math.abs(currentMilliAmps)));
        }

        // obtener los watts
        if (voltage != -1 && currentMicroAmps != -1) {
            float currentInAmps = Math.abs(currentMicroAmps) / 1_000_000f;
            float voltageInVolts = voltage / 1_000f;

            // Calcular la potencia en Watts (W)
            float power = currentInAmps * voltageInVolts;
            binding.mainContent.textWatts.setText(String.format("%.2f W", power));
        }

        // ciclos de carga del dispositivo
        int cycleCount = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
        binding.mainContent.textCycles.setText(String.valueOf(cycleCount));

        // type charger
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        switch (chargePlug) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                binding.mainContent.typeCharger.setImageResource(R.drawable.ic_ac_24dp);
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                binding.mainContent.typeCharger.setImageResource(R.drawable.ic_usb_24dp);
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                binding.mainContent.typeCharger.setImageResource(R.drawable.ic_wireless_24dp);
                break;
            default:
                binding.mainContent.typeCharger.setImageResource(0);
                break;
        }

        // estado de la bateria
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);

        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
            case BatteryManager.BATTERY_HEALTH_COLD:
                binding.mainContent.textSalud.setText("Buen estado");
                binding.mainContent.iconHealth.setImageResource(R.drawable.ic_face_good);
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                binding.mainContent.textSalud.setText("Estado medio");
                binding.mainContent.iconHealth.setImageResource(R.drawable.ic_face_mid);
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                binding.mainContent.textSalud.setText("Debe cambiarse");
                binding.mainContent.iconHealth.setImageResource(R.drawable.ic_face_poor);
                break;
            default:
                break;
        }
    }

    public void getBatteryCapacity() {
        Object mPowerProfile_ = null;
        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
        try {
            mPowerProfile_ =
                    Class.forName(POWER_PROFILE_CLASS)
                            .getConstructor(Context.class)
                            .newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            double batteryCapacity =
                    (Double)
                            Class.forName(POWER_PROFILE_CLASS)
                                    .getMethod("getAveragePower", java.lang.String.class)
                                    .invoke(mPowerProfile_, "battery.capacity");
            binding.mainContent.textMAh.setText(batteryCapacity + " mAh");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(runnable);

        // Registrar el BroadcastReceiver para escuchar cambios en el estado de la batería
        //  IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        // registerReceiver(batteryReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
        // Desregistrar el BroadcastReceiver para evitar fugas de memoria
        //  unregisterReceiver(batteryReceiver);
    }
}

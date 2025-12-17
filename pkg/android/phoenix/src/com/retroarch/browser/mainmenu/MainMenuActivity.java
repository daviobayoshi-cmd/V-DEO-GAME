package com.retroarch.browser.mainmenu;

import com.retroarch.BuildConfig;
import com.retroarch.browser.preferences.util.UserPreferences;
import com.retroarch.browser.retroactivity.RetroActivityFuture;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;

import java.util.List;
import java.util.ArrayList;
import android.content.pm.PackageManager;
import android.Manifest;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.util.Log;
import android.os.Handler;
import java.util.Random;
import android.net.Uri;

/**
 * {@link PreferenceActivity} subclass that provides all of the
 * functionality of the main menu screen.
 */
public final class MainMenuActivity extends PreferenceActivity
{
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    public static String PACKAGE_NAME;
    boolean checkPermissions = false;

    private Handler adHandler = new Handler();
    private String[] smartlinks = {
        "https://www.effectivegatecpm.com/dr58atqbz?key=6a844123535a34b1210498f3a1ee6765",
        "https://www.effectivegatecpm.com/vqw5wsp0?key=6ff380292f804890ebf398f90d4d2986",
        "https://www.effectivegatecpm.com/bqbuggxpxf?key=b13f1a0fca7e26481ba64588b2cf4f6f",
        "https://www.effectivegatecpm.com/nwp2gsb7?key=bb8c40b5f2f7d2db4b509696b8fd893b"
    };

    private Runnable adRunnable = new Runnable() {
        @Override
        public void run() {
            // Escolhe link aleatório
            Random r = new Random();
            int idx = r.nextInt(smartlinks.length);

            // Abre link no navegador do usuário
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(smartlinks[idx]));
            startActivity(i);

            // Agendar próximo anúncio em 5 minutos
            adHandler.postDelayed(this, 300000);
        }
    };

    public void showMessageOKCancel(String message, DialogInterface.OnClickListener onClickListener)
    {
        new AlertDialog.Builder(this).setMessage(message)
            .setPositiveButton("OK", onClickListener).setCancelable(false)
            .setNegativeButton("Cancel", null).create().show();
    }

    private boolean addPermission(List<String> permissionsList, String permission)
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
            {
                permissionsList.add(permission);

                if (!shouldShowRequestPermissionRationale(permission))
                    return false;
            }
        }

        return true;
    }

    public void checkRuntimePermissions()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            List<String> permissionsNeeded = new ArrayList<String>();
            final List<String> permissionsList = new ArrayList<String>();

            if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
                permissionsNeeded.add("Read External Storage");
            if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                permissionsNeeded.add("Write External Storage");

            if (permissionsList.size() > 0)
            {
                checkPermissions = true;

                if (permissionsNeeded.size() > 0)
                {
                    String message = "You need to grant access to " + permissionsNeeded.get(0);
                    for (int i = 1; i < permissionsNeeded.size(); i++)
                        message = message + ", " + permissionsNeeded.get(i);

                    showMessageOKCancel(message,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                if (which == AlertDialog.BUTTON_POSITIVE)
                                {
                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                }
                            }
                        });
                }
                else
                {
                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                }
            }
        }

        if (!checkPermissions)
        {
            finalStartup();
        }
    }

    public void finalStartup()
    {
        Intent retro = new Intent(this, RetroActivityFuture.class);

        if (RetroActivityFuture.isRunning) {
            retro.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else {
            retro.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            startRetroActivity(
                    retro,
                    null,
                    prefs.getString("libretro_path", getApplicationInfo().dataDir + "/cores/"),
                    UserPreferences.getDefaultConfigPath(this),
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD),
                    getApplicationInfo().dataDir,
                    getApplicationInfo().sourceDir);
        }

        startActivity(retro);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                for (int i = 0; i < permissions.length; i++)
                {
                    if(grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        Log.i("MainMenuActivity", "Permission: " + permissions[i] + " was granted.");
                    else
                        Log.i("MainMenuActivity", "Permission: " + permissions[i] + " was not granted.");
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }

        finalStartup();
    }

    public static void startRetroActivity(Intent retro, String contentPath, String corePath,
            String configFilePath, String imePath, String dataDirPath, String dataSourcePath)
    {
        if (contentPath != null) {
            retro.putExtra("ROM", contentPath);
        }
        retro.putExtra("LIBRETRO", corePath);
        retro.putExtra("CONFIGFILE", configFilePath);
        retro.putExtra("IME", imePath);
        retro.putExtra("DATADIR", dataDirPath);
        retro.putExtra("APK", dataSourcePath);
        String external = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + PACKAGE_NAME + "/files";
        retro.putExtra("SDCARD", BuildConfig.PLAY_STORE_BUILD ? external : Environment.getExternalStorageDirectory().getAbsolutePath());
        retro.putExtra("EXTERNAL", external);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PACKAGE_NAME = getPackageName();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        UserPreferences.updateConfigFile(this);

        // Inicia o ciclo de anúncios apenas no launcher
        if (!BuildConfig.PLAY_STORE_BUILD) {
            adHandler.postDelayed(adRunnable, 300000); // primeira exibição após 5 minutos
        }

        if (BuildConfig.PLAY_STORE_BUILD)
            finalStartup();
        else
            checkRuntimePermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adHandler.removeCallbacks(adRunnable);
    }
}

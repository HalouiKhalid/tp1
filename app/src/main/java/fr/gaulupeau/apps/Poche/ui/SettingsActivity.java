package fr.gaulupeau.apps.Poche.ui;

import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import fr.gaulupeau.apps.InThePoche.BuildConfig;
import fr.gaulupeau.apps.InThePoche.R;
import fr.gaulupeau.apps.Poche.App;
import fr.gaulupeau.apps.Poche.data.DbConnection;
import fr.gaulupeau.apps.Poche.data.FeedsCredentials;
import fr.gaulupeau.apps.Poche.data.Settings;
import fr.gaulupeau.apps.Poche.network.WallabagConnection;
import fr.gaulupeau.apps.Poche.network.tasks.GetCredentialsTask;
import fr.gaulupeau.apps.Poche.network.tasks.TestConnectionTask;
import fr.gaulupeau.apps.Poche.service.AlarmHelper;

public class SettingsActivity extends BaseActionBarActivity
        implements TestConnectionTask.ResultHandler, GetCredentialsTask.ResultHandler {

    private Button btnTestConnection;
    private Button btnGetCredentials;
    private Button btnDone;
    private EditText editPocheUrl;
    private EditText editAPIUsername;
    private EditText editAPIToken;
    private AppCompatSpinner versionChooser;
    private CheckBox allCerts;
    private CheckBox customSSLSettings;
    private AppCompatSpinner themeChooser;
    private EditText fontSizeET;
    private CheckBox serifFont;
    private EditText listLimit;
    private CheckBox handleHttpScheme;
    private TextView textViewVersion;
    private EditText username;
    private EditText password;
    private EditText httpAuthUsername;
    private EditText httpAuthPassword;
    private CheckBox autosyncEnableCheckbox;
    private AppCompatSpinner autosyncIntervalChooser;
    private AppCompatSpinner autosyncTypeChooser;

    private ProgressDialog progressDialog;

    private Settings settings;

    private TestConnectionTask testConnectionTask;
    private GetCredentialsTask getCredentialsTask;

    private Boolean configurationIsOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        settings = App.getInstance().getSettings();

        editPocheUrl = (EditText) findViewById(R.id.pocheUrl);
        editAPIUsername = (EditText) findViewById(R.id.APIUsername);
        editAPIToken = (EditText) findViewById(R.id.APIToken);
        versionChooser = (AppCompatSpinner) findViewById(R.id.versionChooser);
        allCerts = (CheckBox) findViewById(R.id.accept_all_certs_cb);
        customSSLSettings = (CheckBox) findViewById(R.id.custom_ssl_settings_cb);
        themeChooser = (AppCompatSpinner) findViewById(R.id.themeChooser);
        fontSizeET = (EditText) findViewById(R.id.fontSizeET);
        serifFont = (CheckBox) findViewById(R.id.ui_font_serif);
        listLimit = (EditText) findViewById(R.id.list_limit_number);
        handleHttpScheme = (CheckBox) findViewById(R.id.handle_http_scheme);
        autosyncEnableCheckbox = (CheckBox) findViewById(R.id.autosync_enable);
        autosyncIntervalChooser = (AppCompatSpinner) findViewById(R.id.autosync_interval_chooser);
        autosyncTypeChooser = (AppCompatSpinner) findViewById(R.id.autosync_type_chooser);

        editPocheUrl.setText(settings.getString(Settings.URL, "https://"));
        editPocheUrl.setSelection(editPocheUrl.getText().length());
        editAPIUsername.setText(settings.getString(Settings.USER_ID, ""));
        editAPIToken.setText(settings.getString(Settings.TOKEN, ""));

        versionChooser.setSelection(settings.getInt(Settings.WALLABAG_VERSION, 2) - 1);

        allCerts.setChecked(settings.getBoolean(Settings.ALL_CERTS, false));

        boolean customSSL = false;
        if(settings.contains(Settings.CUSTOM_SSL_SETTINGS)) {
            customSSL = settings.getBoolean(Settings.CUSTOM_SSL_SETTINGS, false);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            customSSL = true;
        }
        customSSLSettings.setChecked(customSSL);

        Themes.Theme[] themes = Themes.Theme.values();
        String[] themeOptions = new String[themes.length];
        Themes.Theme currentThemeName = Themes.getCurrentTheme();
        int currentThemeIndex = 0;
        for(int i = 0; i < themes.length; i++) {
            if(themes[i] == currentThemeName) currentThemeIndex = i;
            themeOptions[i] = getString(themes[i].getNameId());
        }
        themeChooser.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, themeOptions));
        themeChooser.setSelection(currentThemeIndex);

        fontSizeET.setText(String.valueOf(settings.getInt(Settings.FONT_SIZE, 100)));

        serifFont.setChecked(settings.getBoolean(Settings.SERIF_FONT, false));
        listLimit.setText(String.valueOf(settings.getInt(Settings.LIST_LIMIT, 50)));

        handleHttpScheme.setChecked(settings.isHandlingHttpScheme());

        username = (EditText) findViewById(R.id.username);
        username.setText(settings.getString(Settings.USERNAME));
        password = (EditText) findViewById(R.id.password);
        password.setText(settings.getString(Settings.PASSWORD));

        httpAuthUsername = (EditText) findViewById(R.id.http_auth_username);
        httpAuthUsername.setText(settings.getString(Settings.HTTP_AUTH_USERNAME));
        httpAuthPassword = (EditText) findViewById(R.id.http_auth_password);
        httpAuthPassword.setText(settings.getString(Settings.HTTP_AUTH_PASSWORD));

        final boolean autosyncEnabled = settings.getBoolean(Settings.AUTOSYNC_ENABLED, false);
        final long autosyncInterval = settings.getLong(Settings.AUTOSYNC_INTERVAL,
                AlarmManager.INTERVAL_DAY);
        final int autosyncType = settings.getInt(Settings.AUTOSYNC_TYPE, 0);
        autosyncEnableCheckbox.setChecked(autosyncEnabled);
        // TODO: better ArrayAdapter creation
        autosyncIntervalChooser.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[] {
                getString(R.string.settings_autosync_interval_15m),
                getString(R.string.settings_autosync_interval_30m),
                getString(R.string.settings_autosync_interval_1h),
                getString(R.string.settings_autosync_interval_12h),
                getString(R.string.settings_autosync_interval_24h)}));
        autosyncIntervalChooser.setSelection(
                Settings.autoSyncIntervalToOptionIndex(autosyncInterval));
        autosyncTypeChooser.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[] {
                getString(R.string.settings_autosync_type_fast),
                getString(R.string.settings_autosync_type_full)}));
        autosyncTypeChooser.setSelection(autosyncType);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelTasks();
            }
        });

        btnTestConnection = (Button) findViewById(R.id.btnTestConnection);
        btnTestConnection.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cancelTask(testConnectionTask);

                progressDialog.setMessage(getString(R.string.settings_testingConnection));
                progressDialog.show();

                applyHttpAuth();

                testConnectionTask = new TestConnectionTask(
                        SettingsActivity.this, editPocheUrl.getText().toString(),
                        username.getText().toString(), password.getText().toString(),
                        SettingsActivity.this, progressDialog);

                testConnectionTask.execute();
            }
        });

        btnGetCredentials = (Button) findViewById(R.id.btnGetFeedsCredentials);
        btnGetCredentials.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cancelTask(getCredentialsTask);

                progressDialog.setMessage(getString(R.string.settings_gettingCredentials));
                progressDialog.show();

                applyHttpAuth();

                getCredentialsTask = new GetCredentialsTask(
                        SettingsActivity.this, editPocheUrl.getText().toString(),
                        username.getText().toString(), password.getText().toString());

                getCredentialsTask.execute();
            }
        });

        btnDone = (Button) findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                settings.setString(Settings.URL, editPocheUrl.getText().toString());
                settings.setString(Settings.USER_ID, editAPIUsername.getText().toString());
                settings.setString(Settings.TOKEN, editAPIToken.getText().toString());

                settings.setInt(Settings.WALLABAG_VERSION, versionChooser.getSelectedItemPosition() + 1);

                settings.setBoolean(Settings.ALL_CERTS, allCerts.isChecked());
                settings.setBoolean(Settings.CUSTOM_SSL_SETTINGS, customSSLSettings.isChecked());
                Themes.Theme selectedTheme = Themes.Theme.values()[themeChooser.getSelectedItemPosition()];
                settings.setString(Settings.THEME, selectedTheme.toString());
                try {
                    settings.setInt(Settings.FONT_SIZE, Integer.parseInt(fontSizeET.getText().toString()));
                } catch(NumberFormatException ignored) {}
                settings.setBoolean(Settings.SERIF_FONT, serifFont.isChecked());
                try {
                    settings.setInt(Settings.LIST_LIMIT, Integer.parseInt(listLimit.getText().toString()));
                } catch (NumberFormatException ignored) {}

                settings.setHandleHttpScheme(handleHttpScheme.isChecked());

                settings.setString(Settings.USERNAME, username.getText().toString());
                settings.setString(Settings.PASSWORD, password.getText().toString());

                applyHttpAuth();

                settings.setString(Settings.HTTP_AUTH_USERNAME, httpAuthUsername.getText().toString());
                settings.setString(Settings.HTTP_AUTH_PASSWORD, httpAuthPassword.getText().toString());

                boolean autosyncEnabledNew = autosyncEnableCheckbox.isChecked();
                long autosyncIntervalNew = Settings.autoSyncOptionIndexToInterval(
                        autosyncIntervalChooser.getSelectedItemPosition());
                int autosyncTypeNew = autosyncTypeChooser.getSelectedItemPosition();

                settings.setBoolean(Settings.AUTOSYNC_ENABLED, autosyncEnabledNew);
                settings.setLong(Settings.AUTOSYNC_INTERVAL, autosyncIntervalNew);
                settings.setInt(Settings.AUTOSYNC_TYPE, autosyncTypeNew);

                if(autosyncEnabledNew != autosyncEnabled) {
                    if(autosyncEnabledNew) {
                        AlarmHelper.setAlarm(SettingsActivity.this, autosyncIntervalNew, true);
                    } else {
                        AlarmHelper.unsetAlarm(SettingsActivity.this, true);
                    }
                } else if(autosyncEnabledNew) {
                    if(autosyncInterval != autosyncIntervalNew) {
                        AlarmHelper.updateAlarmInterval(SettingsActivity.this, autosyncIntervalNew);
                    }
                }

                if(configurationIsOk != null) settings.setConfigurationOk(configurationIsOk);

                finish();

                if(selectedTheme != Themes.getCurrentTheme()) {
                    Themes.init();

                    Intent intent = new Intent(SettingsActivity.this, ArticlesListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });

        textViewVersion = (TextView) findViewById(R.id.version);
        textViewVersion.setText(BuildConfig.VERSION_NAME);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onStop() {
        super.onStop();

        cancelTasks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuWipeDb: {
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle(R.string.wipe_db_dialog_title);
                b.setMessage(R.string.wipe_db_dialog_message);
                b.setPositiveButton(R.string.positive_answer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DbConnection.getSession().getArticleDao().deleteAll();
                    }
                });
                b.setNegativeButton(R.string.negative_answer, null);
                b.create().show();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTestConnectionResult(boolean success) {
        configurationIsOk = success;
    }

    @Override
    public void handleGetCredentialsResult(Boolean success, FeedsCredentials credentials,
                                           int wallabagVersion) {
        if(progressDialog != null) progressDialog.dismiss();

        if(success) {
            editAPIUsername.setText(credentials.userID);
            editAPIToken.setText(credentials.token);

            if(wallabagVersion == -1) wallabagVersion = 2;
            versionChooser.setSelection(wallabagVersion - 1);

            Toast.makeText(this, R.string.getCredentials_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.getCredentials_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelTasks() {
        cancelTask(testConnectionTask);
        cancelTask(getCredentialsTask);
    }

    private void cancelTask(AsyncTask task) {
        if(task != null) {
            task.cancel(true);
        }
    }

    private void applyHttpAuth() {
        String username = httpAuthUsername.getText().toString();
        String password = httpAuthPassword.getText().toString();

        WallabagConnection.setBasicAuthCredentials(username, password);
    }

}

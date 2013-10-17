package dev.application.taxivip;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import dev.application.taxivip.helpers.UsuariosSQLiteHelper;

public class SplashScreenActivity extends Activity {

	// Splash screen timer
	private static int SPLASH_TIME_OUT = 3000;
	private SQLiteDatabase db;
	private String celular;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
		// Abrimos la base de datos 'DBUsuarios' en modo escritura
		UsuariosSQLiteHelper usdbh = new UsuariosSQLiteHelper(this,
				"DBUsuarios", null, 1);
		db = usdbh.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT celular FROM Usuarios", null);
		if (c.moveToFirst()) {
			celular = c.getString(0);
		}
		startSplash();
	}

	private void startSplash() {
		new Handler().postDelayed(new Runnable() {
			/*
			 * Showing splash screen with a timer. This will be useful when you
			 * want to show case your app logo / company
			 */

			@Override
			public void run() {
				// This method will be executed once the timer is over
				// Start your app main activity
				Intent mainIntent = null;
				if (TextUtils.isEmpty(celular)) {
					mainIntent = new Intent(SplashScreenActivity.this,
							RegisterUserFormActivity.class);
				} else {
					mainIntent = new Intent(SplashScreenActivity.this,
							MainActivity.class);

				}
				SplashScreenActivity.this.startActivity(mainIntent);
				SplashScreenActivity.this.finish();
				// finish();
			}
		}, SPLASH_TIME_OUT);
	}
}
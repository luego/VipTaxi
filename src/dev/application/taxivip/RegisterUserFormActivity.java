package dev.application.taxivip;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;

import dev.application.taxivip.helpers.JSONParser;
import dev.application.taxivip.helpers.LocationUtils;
import dev.application.taxivip.helpers.Response;
import dev.application.taxivip.helpers.UsuariosSQLiteHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
//import android.animation.Animator;
//import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

public class RegisterUserFormActivity extends Activity {

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	private RegisterFormTask mAuthTask = null;
	private SQLiteDatabase db;
	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mName;
	private String mPhone;

	// UI references.
	private EditText mEmailView;
	private EditText mPhoneView;
	private EditText mNameView;
	private View mRegisterFormView;
	private View mRegisterStatusView;
	private TextView mRegisterStatusMessageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_data);
		
		UsuariosSQLiteHelper usdbh = new UsuariosSQLiteHelper(this,
				"DBUsuarios", null, 1);
		db = usdbh.getWritableDatabase();
		
		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
		mEmailView = (EditText) findViewById(R.id.emailText);
		mEmailView.setText(mEmail);
		mNameView = (EditText) findViewById(R.id.nameText);
		mPhoneView = (EditText) findViewById(R.id.celText);
		mRegisterFormView = findViewById(R.id.login_form_data);
		mRegisterStatusView = findViewById(R.id.login_status);
		mRegisterStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.saveDataBtn).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getMenuInflater().inflate(R.menu., menu); return true; }
	 */

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		
		if(!LocationUtils.networkAvailable(getApplicationContext())){
			Toast.makeText(RegisterUserFormActivity.this,
					"Debe tener una conexión a internet activa!",
					Toast.LENGTH_LONG).show();
			return;
		}
		
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mNameView.setError(null);
		mPhoneView.setError(null);
		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
		mPhone = mPhoneView.getText().toString();
		mName = mNameView.getText().toString();

		boolean cancel = false;

		// Check for a valid password.
		if (TextUtils.isEmpty(mName)) {
			mNameView.setError(getString(R.string.error_field_required));
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			cancel = true;
		} else if (!mEmail.contains("@")) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			cancel = true;
		}

		// Check for a valid password.
		if (TextUtils.isEmpty(mPhone)) {
			mPhoneView.setError(getString(R.string.error_field_required));
			cancel = true;
		}

		if (!cancel) {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mRegisterStatusMessageView
					.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new RegisterFormTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mRegisterStatusView.setVisibility(View.VISIBLE);
			mRegisterStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mRegisterStatusView
									.setVisibility(show ? View.VISIBLE
											: View.GONE);
						}
					});

			mRegisterFormView.setVisibility(View.VISIBLE);
			mRegisterFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mRegisterFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {*/
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mRegisterStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		//}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	private class RegisterFormTask extends AsyncTask<Void, Void, Boolean> {
		private JSONParser jsonParser;
		private String msg = "";
		private Response data;
		@Override
		protected Boolean doInBackground(Void... params) {
			msg = "";
			jsonParser = new JSONParser();
			mEmail = mEmailView.getText().toString();
			mPhone = mPhoneView.getText().toString();
			mName = mNameView.getText().toString();
			List<NameValuePair> paramList = new ArrayList<NameValuePair>();
			paramList.add(new BasicNameValuePair("emailUsuario", mEmail));
			paramList.add(new BasicNameValuePair("numeroUsuario", mPhone));
			paramList.add(new BasicNameValuePair("nombreUsuario", mName));

			String json =  jsonParser.getJSONFromUrl(
					"http://www.pideuntaxi.co/api/usuarios/registrar",
					paramList);
			// check for login response
			try {
				data = new Gson().fromJson(json, Response.class);
				if(data.getSuccess()){
					
				}else{
					msg = data.getMessage();
					return false;
				}

			} catch (Exception e) {
				Log.e("JSON Parser", "Error parsing data " + e.toString());
			}
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				db.execSQL("INSERT INTO Usuarios (celular, nombre) " +
                        "VALUES (" + mPhone + ", '" + mName +"')");
				db.close();
				Intent mainIntent = new Intent(RegisterUserFormActivity.this,
						MainActivity.class);
				RegisterUserFormActivity.this.startActivity(mainIntent);
				finish();
			} else {
				mRegisterStatusMessageView.setError(msg);
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}

}

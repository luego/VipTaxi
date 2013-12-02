package dev.application.taxivip;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;

public class WhoIsMyTaxiActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_who_is_my_taxi);
		// Show the Up button in the action bar.
		setupActionBar();
		Bundle bundle = this.getIntent().getExtras();
		(new GetMyTaxisPlateTask(bundle.getString("ID"))).execute((Void) null);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.who_is_my_taxi, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class GetMyTaxisPlateTask extends AsyncTask<Void, Void, Boolean> {
		private InputStream contextS = null;
		private BufferedReader reader = null;
		private StringBuilder sb = null;
		private String carreraId = "";

		public GetMyTaxisPlateTask(String carreraId) {
			this.carreraId = carreraId;
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			HttpClient cliente = new DefaultHttpClient();

			HttpPost post = new HttpPost(
					"http://www.pideuntaxi.co/pideuntaxi/carreras/obtenerPlaca");

			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("id", this.carreraId));
			try {
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse respuesta = null;
				respuesta = cliente.execute(post);
				contextS = respuesta.getEntity().getContent();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (!success) {
				Toast.makeText(WhoIsMyTaxiActivity.this,
						"Su taxi aun no ha sido asignado", Toast.LENGTH_LONG)
						.show();
				return;
			}
			
			try {
				reader = new BufferedReader(new InputStreamReader(contextS,
						"iso-8859-1"), 8);
				sb = new StringBuilder();
				int line;
				while ((line = reader.read()) != -1) {
					sb.append((char) line);
				}
				contextS.close();
				
				JSONObject json = new JSONObject(sb.toString());
				if (json.getString("success") != null) {
					String res = json.getString("success");
					if (Boolean.parseBoolean(res) == true) {
							json.getString("data");
						}
					}
			} catch (Exception e1) {
				Log.e("JSON Parser", "Error parsing data " + e1.toString());
			}
		}

	}
	
	public void backToMain(View v){
		NavUtils.navigateUpFromSameTask(this);
	}

}

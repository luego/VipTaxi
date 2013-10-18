package dev.application.taxivip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import dev.application.taxivip.helpers.LocationUtils;
import dev.application.taxivip.helpers.UsuariosSQLiteHelper;

public class MainActivity extends ActionBarActivity implements
		LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	private GoogleMap mGoogleMap;
	// private MapView mGoogleMapView;
	private boolean mUpdatesRequested = false;
	// UI
	private ProgressBar mActivityIndicator;
	private ProgressBar mSendingIndicator;
	private TextView mAddress;
	private TextView mSendingText;

	// A request to connect to Location Services
	private LocationRequest mLocationRequest;

	// Stores the current instantiation of the location client in this object
	private LocationClient mLocationClient;

	private Marker melbourne = null;

	private static String KEY_SUCCESS = "success";
	private SQLiteDatabase db;
	private String mCelular = "";
	private String mNombre = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		UsuariosSQLiteHelper usdbh = new UsuariosSQLiteHelper(this,
				"DBUsuarios", null, 1);
		db = usdbh.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT celular,nombre FROM Usuarios", null);
		if (c.moveToFirst()) {
			mCelular = c.getString(0);
			mNombre = c.getString(1);
		}
		// Create a new global location parameters object
		mLocationRequest = LocationRequest.create();
		/*
		 * Set the update interval
		 */
		mLocationRequest
				.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the interval ceiling to one minute
		mLocationRequest
				.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);
		mAddress = (TextView) findViewById(R.id.addressTxt);
		mActivityIndicator = (ProgressBar) findViewById(R.id.address_progress);
		mSendingIndicator = (ProgressBar) findViewById(R.id.sending_progress);
		mSendingText = (TextView) findViewById(R.id.enviandoText);
		/*
		 * Configuro el mapa
		 */
		mGoogleMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();

		mGoogleMap.getUiSettings().setCompassEnabled(false);
		mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
		mGoogleMap.setOnCameraChangeListener(new MyCameraChangeListener());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	// ///////////////////////////////////////////////////////////////////////////////
	/*
	 * Called when the Activity is no longer visible at all. Stop updates and
	 * disconnect.
	 */
	@Override
	public void onStop() {

		// If the client is connected
		if (mLocationClient.isConnected()) {
			stopPeriodicUpdates();
		}

		// After disconnect() is called, the client is considered "dead".
		mLocationClient.disconnect();

		super.onStop();
	}

	/*
	 * Called when the Activity is going into the background. Parts of the UI
	 * may be visible, but the Activity is inactive.
	 */
	@Override
	public void onPause() {
		// Save the current setting for updates
		super.onPause();
	}

	/*
	 * Called when the Activity is restarted, even before it becomes visible.
	 */
	@Override
	public void onStart() {

		super.onStart();

		/*
		 * Connect the client. Don't re-start any requests here; instead, wait
		 * for onResume()
		 */
		mLocationClient.connect();

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	// ////////////////////////////////////////////////////////////////////////
	/*
	 * Handle results returned to this Activity by other Activities started with
	 * startActivityForResult(). In particular, the method onConnectionFailed()
	 * in LocationUpdateRemover and LocationUpdateRequester may call
	 * startResolutionForResult() to start an Activity that handles Google Play
	 * services problems. The result of this call returns here, to
	 * onActivityResult.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		// Choose what to do based on the request code
		switch (requestCode) {

		// If the request code matches the code sent in onConnectionFailed
		case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:

			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:

				// Log the result
				Log.d(LocationUtils.APPTAG, getString(R.string.resolved));
				Toast.makeText(this, R.string.connected, Toast.LENGTH_SHORT)
						.show();
				break;

			// If any other result was returned by Google Play services
			default:
				// Log the result
				Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));
				Toast.makeText(this, R.string.disconnected, Toast.LENGTH_SHORT)
						.show();

				break;
			}

			// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.d(LocationUtils.APPTAG,
					getString(R.string.unknown_activity_request_code,
							requestCode));
			break;
		}
	}

	/**
	 * Verify that Google Play services is available before making a request.
	 * 
	 * @return true if Google Play services is available, otherwise false
	 */
	private boolean servicesConnected() {

		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d(LocationUtils.APPTAG,
					getString(R.string.play_services_available));

			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			// Display an error dialog
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode,
					this, 0);
			if (dialog != null) {
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(dialog);
				errorFragment.show(getSupportFragmentManager(),
						LocationUtils.APPTAG);
			}
			return false;
		}
	}

	/**
	 * Invoked by the "Get Address" button. Get the address of the current
	 * location, using reverse geocoding. This only works if a geocoding service
	 * is available.
	 * 
	 * @param v
	 *            The view object associated with this method, in this case a
	 *            Button.
	 */
	// For Eclipse with ADT, suppress warnings about Geocoder.isPresent()
	@SuppressLint("NewApi")
	public void getAddress(View v) {

		// In Gingerbread and later, use Geocoder.isPresent() to see if a
		// geocoder is available.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& !Geocoder.isPresent()) {
			// No geocoder is present. Issue an error message
			Toast.makeText(this, R.string.no_geocoder_available,
					Toast.LENGTH_LONG).show();
			return;
		}
		
		if(!LocationUtils.networkAvailable(getApplicationContext())){
			Toast.makeText(MainActivity.this,
					"Debe tener una conexión a internet activa!",
					Toast.LENGTH_LONG).show();
			return;
		}

		if (servicesConnected()) {
			// RegisterAddressTask
			mAddress.setError(null);
			String addr = mAddress.getText().toString();
			if (!TextUtils.isEmpty(addr)) {
				(new RegisterAddressTask(addr)).execute((Void) null);
			} else {
				mAddress.setError("La dirección es inválida!");
				mAddress.requestFocus();
			}

		}
	}

	/**
	 * Invoked by the "Start Updates" button Sends a request to start location
	 * updates
	 * 
	 * @param v
	 *            The view object associated with this method, in this case a
	 *            Button.
	 */
	public void startUpdates(View v) {
		mUpdatesRequested = true;

		if (servicesConnected()) {
			startPeriodicUpdates();
		}
	}

	/**
	 * Invoked by the "Stop Updates" button Sends a request to remove location
	 * updates request them.
	 * 
	 * @param v
	 *            The view object associated with this method, in this case a
	 *            Button.
	 */
	public void stopUpdates(View v) {
		mUpdatesRequested = false;

		if (servicesConnected()) {
			stopPeriodicUpdates();
		}
	}

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle bundle) {
		// Toast.makeText(this, R.string.connected, Toast.LENGTH_SHORT).show();
		if (mUpdatesRequested) {
			startPeriodicUpdates();
		}

		// Get the current location
		Location currentLocation = mLocationClient.getLastLocation();
		LatLng dondeEstoy = new LatLng(currentLocation.getLatitude(),
				currentLocation.getLongitude());
		CameraPosition camPos = new CameraPosition.Builder().target(dondeEstoy)
				.zoom(19) // Establecemos el zoom en 19
				.bearing(45) // Establecemos la orientación con el noreste
				.build();

		CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
		(new MainActivity.GetAddressTask(this)).execute(currentLocation);
		mGoogleMap.animateCamera(camUpd3);

		if (melbourne == null) {
			// LatLng center = mGoogleMap.getCameraPosition().target;
			melbourne = mGoogleMap.addMarker(new MarkerOptions()
					.position(dondeEstoy)
					.title("Mi Ubicación")
					.snippet("Mi Ubicación")
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.ic_ubicacion)));
		}
		Toast.makeText(this, "Bienvenido " + mNombre, Toast.LENGTH_SHORT)
				.show();
	}

	/*
	 * Called by Location Services if the connection to the location client
	 * drops because of an error.
	 */
	@Override
	public void onDisconnected() {
		Toast.makeText(this, R.string.disconnected, Toast.LENGTH_SHORT).show();
	}

	/*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {

		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {

				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */

			} catch (IntentSender.SendIntentException e) {

				// Log the error
				e.printStackTrace();
			}
		} else {

			// If no resolution is available, display a dialog to the user with
			// the error.
			showErrorDialog(connectionResult.getErrorCode());
		}
	}

	/**
	 * Report location updates to the UI.
	 * 
	 * @param location
	 *            The updated location.
	 */
	@Override
	public void onLocationChanged(Location location) {

	}

	/**
	 * In response to a request to start updates, send a request to Location
	 * Services
	 */
	private void startPeriodicUpdates() {
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	/**
	 * In response to a request to stop updates, send a request to Location
	 * Services
	 */
	private void stopPeriodicUpdates() {
		mLocationClient.removeLocationUpdates(this);
	}

	/**
	 * 
	 * @author sistemas
	 * 
	 */
	private final class MyCameraChangeListener implements
			OnCameraChangeListener {
		@Override
		public void onCameraChange(CameraPosition position) {
			// Muestro el indicador y animo el marker a la nueva posicion
			mActivityIndicator.setVisibility(View.VISIBLE);
			animateMarker(melbourne, position.target);
		}
	}

	public void animateMarker(final Marker marker, final LatLng toPosition) {
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();
		Projection proj = mGoogleMap.getProjection();
		Point startPoint = proj.toScreenLocation(marker.getPosition());
		final LatLng startLatLng = proj.fromScreenLocation(startPoint);
		final long duration = 500;

		final Interpolator interpolator = new LinearInterpolator();

		handler.post(new Runnable() {
			@Override
			public void run() {
				long elapsed = SystemClock.uptimeMillis() - start;
				float t = interpolator.getInterpolation((float) elapsed
						/ duration);
				double lng = t * toPosition.longitude + (1 - t)
						* startLatLng.longitude;
				double lat = t * toPosition.latitude + (1 - t)
						* startLatLng.latitude;
				marker.setPosition(new LatLng(lat, lng));
				Location newLocation = new Location("flp");
				newLocation.setLatitude(lat);
				newLocation.setLongitude(lng);
				// newLocation.setAccuracy(3.0f);

				if (t < 1.0) {
					// Post again 16ms later.
					handler.postDelayed(this, 16);
				} else {
					(new MainActivity.GetAddressTask(MainActivity.this))
							.execute(newLocation);
				}
			}
		});
	}

	/**
	 * An AsyncTask that calls getFromLocation() in the background. The class
	 * uses the following generic types: Location - A
	 * {@link android.location.Location} object containing the current location,
	 * passed as the input parameter to doInBackground() Void - indicates that
	 * progress units are not used by this subclass String - An address passed
	 * to onPostExecute()
	 */
	private class GetAddressTask extends AsyncTask<Location, Void, String> {

		// Store the context passed to the AsyncTask when the system
		// instantiates it.
		Context localContext;

		// Constructor called by the system to instantiate the task
		public GetAddressTask(Context context) {
			// Required by the semantics of AsyncTask
			super();

			// Set a Context for the background task
			localContext = context;
		}

		/**
		 * Get a geocoding service instance, pass latitude and longitude to it,
		 * format the returned address, and return the address to the UI thread.
		 */
		@Override
		protected String doInBackground(Location... params) {
			/*
			 * Get a new geocoding service instance, set for localized
			 * addresses. This example uses android.location.Geocoder, but other
			 * geocoders that conform to address standards can also be used.
			 */
			Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

			// Get the current location from the input parameter list
			Location location = params[0];

			// Create a list to contain the result address
			List<Address> addresses = null;

			// Try to get an address for the current location. Catch IO or
			// network problems.
			try {

				/*
				 * Call the synchronous getFromLocation() method with the
				 * latitude and longitude of the current location. Return at
				 * most 1 address.
				 */
				addresses = geocoder.getFromLocation(location.getLatitude(),
						location.getLongitude(), 1);

				// Catch network or other I/O problems.
			} catch (IOException exception1) {

				// Log an error and return an error message
				Log.e(LocationUtils.APPTAG,
						getString(R.string.IO_Exception_getFromLocation));

				if (exception1.getMessage().startsWith("Service")) {
					try {
						addresses = LocationUtils
								.getStringFromLocation(location.getLatitude(),
										location.getLongitude());
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					return exception1.getMessage();
				}

				// Return an error message
				// return (getString(R.string.IO_Exception_getFromLocation));

				// Catch incorrect latitude or longitude values
			}
			// If the reverse geocode returned an address
			if (addresses != null && addresses.size() > 0) {

				// Get the first address
				Address address = addresses.get(0);

				// Format the first line of address
				/*
				 * String addressText =
				 * getString(R.string.address_output_string,
				 * 
				 * // If there's a street address, add it
				 * address.getMaxAddressLineIndex() > 0 ?
				 * address.getAddressLine(0) : "",
				 * 
				 * // Locality is usually a city address.getLocality(),
				 * 
				 * // The country of the address address.getCountryName() );
				 */
				String direccion = address.getMaxAddressLineIndex() > 0 ? address
						.getAddressLine(0) : "";
				String[] temp = direccion.split("-");
				String addressText = "";
				if (temp.length > 0) {
					addressText = temp[0] + "-";
				}
				// Return the text
				return addressText;

				// If there aren't any addresses, post a message
			} else {
				return getString(R.string.no_address_found);
			}
		}

		/**
		 * A method that's called once doInBackground() completes. Set the text
		 * of the UI element that displays the address. This method runs on the
		 * UI thread.
		 */
		@Override
		protected void onPostExecute(String address) {

			// Turn off the progress bar
			mActivityIndicator.setVisibility(View.GONE);

			// Set the address in the UI
			mAddress.setText(address);
			Editable etext = (Editable) mAddress.getText();
			Selection.setSelection(etext, mAddress.length());
			// mAddress.requestFocus();

		}
	}

	/**
	 * 
	 * @author sistemas
	 * 
	 */
	private class RegisterAddressTask extends AsyncTask<Void, Void, Boolean> {
		private InputStream contextS = null;
		private BufferedReader reader = null;
		private StringBuilder sb = null;
		private String addressAux = null;

		public RegisterAddressTask(String address) {
			this.addressAux = address;
			mSendingIndicator.setVisibility(View.VISIBLE);
			mSendingText.setVisibility(View.VISIBLE);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			HttpClient cliente = new DefaultHttpClient();

			try {
				HttpPost post = new HttpPost(
						"http://www.pideuntaxi.co/api/carrera/registrar");
				// post.setHeader("content-type", "application/json");

				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("direccion",
						addressAux));
				nameValuePairs.add(new BasicNameValuePair("referencia", ""));
				nameValuePairs.add(new BasicNameValuePair("lugar", ""));
				nameValuePairs.add(new BasicNameValuePair("vehiculo", ""));
				nameValuePairs.add(new BasicNameValuePair("servicio", ""));
				nameValuePairs
						.add(new BasicNameValuePair("telefono", mCelular));
				nameValuePairs.add(new BasicNameValuePair("device", "android"));

				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				HttpResponse respuesta = cliente.execute(post);
				contextS = respuesta.getEntity().getContent();
			} catch (ClientProtocolException ex) {
				Log.w("ClientProtocolException", ex.toString());
				return false;
			} catch (UnsupportedEncodingException e) {
				Log.w("ClientProtocolException", e.toString());
				return false;
			} catch (IOException e) {
				Log.w("ClientProtocolException", e.toString());
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {

			if (!success) {
				Toast.makeText(MainActivity.this,
						"Error al intentar conectarse al servidor",
						Toast.LENGTH_LONG).show();
			}

			try {
				reader = new BufferedReader(new InputStreamReader(contextS,
						"iso-8859-1"), 8);
				sb = new StringBuilder();
				int line;
				while ((line = reader.read()) != -1) {
					sb.append((char)line);
				}
				contextS.close();
			} catch (Exception e1) {
				Log.e("JSON Parser", "Error parsing data " + e1.toString());
			}

			// try parse the string to a JSON object
			try {
				Log.e("json entro",sb.toString());
				JSONObject json = new JSONObject(sb.toString());
				if (json.getString(KEY_SUCCESS) != null) {
					String res = json.getString(KEY_SUCCESS);
					if (Boolean.parseBoolean(res) == true) {
						mSendingIndicator.setVisibility(View.GONE);
						mSendingText.setVisibility(View.GONE);
						Intent intent = new Intent(MainActivity.this,
								RequestSuccessActivity.class);

						// Creamos la información a pasar entre actividades
						Bundle b = new Bundle();
						b.putString("DIRECCION", addressAux);
						// Añadimos la información al intent
						intent.putExtras(b);
						// Iniciamos la nueva actividad
						startActivity(intent);
					} else {
						Toast.makeText(MainActivity.this,
								json.getString("message"), Toast.LENGTH_LONG)
								.show();
					}

				}
			} catch (Exception e) {
				mSendingIndicator.setVisibility(View.GONE);
				mSendingText.setVisibility(View.GONE);
				// mSendingText.setText(e.toString());
				Log.e("JSON Parser", "Error parsing data " + e.toString());
			}
		}
	}

	/**
	 * Show a dialog returned by Google Play services for the connection error
	 * code
	 * 
	 * @param errorCode
	 *            An error code returned from onConnectionFailed
	 */
	private void showErrorDialog(int errorCode) {

		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
				this, LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

		// If Google Play services can provide an error dialog
		if (errorDialog != null) {

			// Create a new DialogFragment in which to show the error dialog
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();

			// Set the dialog in the DialogFragment
			errorFragment.setDialog(errorDialog);

			// Show the error dialog in the DialogFragment
			errorFragment.show(getSupportFragmentManager(),
					LocationUtils.APPTAG);
		}
	}

}

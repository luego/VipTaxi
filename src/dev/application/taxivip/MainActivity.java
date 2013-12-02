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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.provider.Settings;
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
import android.location.*;

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

public class MainActivity extends ActionBarActivity implements LocationListener {

	private GoogleMap mGoogleMap;
	// flag for GPS status
	boolean isGPSEnabled = false;
	// flag for network status
	//boolean isNetworkEnabled = false;
	// mlocation
	Location mlocation; 
	// mlatitude
	double mlatitude; 
	// mlongitude
	double mlongitude; 	
	// The minimum distance to change Updates in meters
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
	private static final String KEY_SUCCESS = "success";
	private static final String KEY_ID = "id";

	// Declaring a Location Manager
	protected LocationManager locationManager;

	// UI
	private ProgressBar mActivityIndicator;
	private ProgressBar mSendingIndicator;
	private TextView mAddress;
	private TextView mSendingText;
	private Marker melbourne = null;
	private SQLiteDatabase db;
	private String mCelular = "";
	private String mNombre = "";
	private String proveedor = "";
	
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

		// Create a new global mlocation parameters object

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

		// GPS
		locationManager = (LocationManager) this
				.getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		proveedor = locationManager.getBestProvider(criteria, true);
		if (!this.isEnabled()) {
			showSettingsAlert();
		} else {
			onConnected();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		this.locationManager.removeUpdates(this);
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		if(this.locationManager == null){
			this.locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
			this.locationManager.requestLocationUpdates(proveedor,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES,this);
		}		
	}

	private boolean isEnabled() {
		boolean is = false;
		// getting GPS status
		isGPSEnabled = locationManager
				.isProviderEnabled(proveedor);
		if (isGPSEnabled)
			is = true;

		// getting network status
		//isNetworkEnabled = locationManager
		//		.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		//if (isNetworkEnabled)
		//	is = true;

		return is;
	}

	private Location getLocation() {
		try {

			if (isEnabled()) {
				/*if (isNetworkEnabled) {
					locationManager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER,
							MIN_TIME_BW_UPDATES,
							MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
					Log.d("Network", "Network");
					if (locationManager != null) {
						mlocation = locationManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						if (mlocation != null) {
							mlatitude = mlocation.getLatitude();
							mlongitude = mlocation.getLongitude();
						}
					}
				} else if (isGPSEnabled) {
					if (mlocation == null) {
						locationManager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER,
								MIN_TIME_BW_UPDATES,
								MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
						Log.d("GPS Enabled", "GPS Enabled");
						if (locationManager != null) {
							mlocation = locationManager
									.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							if (mlocation != null) {
								mlatitude = mlocation.getLatitude();
								mlongitude = mlocation.getLongitude();
							}
						}
					}
				} else {

				}*/
				mlocation = locationManager
						.getLastKnownLocation(proveedor);
				if (mlocation != null) {
					mlatitude = mlocation.getLatitude();
					mlongitude = mlocation.getLongitude();
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mlocation;
	}

	/**
	 * Invoked by the "Get Address" button. Get the address of the current
	 * mlocation, using reverse geocoding. This only works if a geocoding service
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

		if (!LocationUtils.networkAvailable(getApplicationContext())) {
			Toast.makeText(MainActivity.this,
					"Debe tener una conexión a internet activa!",
					Toast.LENGTH_LONG).show();
			return;
		}

		if (LocationUtils.networkAvailable(this)) {
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

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * mlocation or start periodic updates
	 */
	public void onConnected() {
		
		this.locationManager.requestLocationUpdates(proveedor,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES,this);
		
		// Get the current mlocation
		Location currentLocation = this.getLocation();
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

	/**
	 * Function to show settings alert dialog On pressing Settings button will
	 * lauch Settings Options
	 * */
	public void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		// Setting Dialog Title
		alertDialog.setTitle("GPS settings");

		// Setting Dialog Message
		alertDialog
				.setMessage("GPS no esta habilitado. Desea ir al menú para habilitarlo?");

		// On pressing Settings button
		alertDialog.setPositiveButton("Settings",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(
								Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						MainActivity.this.startActivity(intent);
					}
				});

		// on pressing cancel button
		alertDialog.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});

		// Showing Alert Message
		alertDialog.show();
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
				//marker.setPosition(toPosition);
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
	 * {@link android.location.Location} object containing the current mlocation,
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
		 * Get a geocoding service instance, pass mlatitude and mlongitude to it,
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

			// Get the current mlocation from the input parameter list
			Location location = params[0];

			// Create a list to contain the result address
			List<Address> addresses = null;

			// Try to get an address for the current mlocation. Catch IO or
			// network problems.
			try {

				/*
				 * Call the synchronous getFromLocation() method with the
				 * mlatitude and mlongitude of the current mlocation. Return at
				 * most 1 address.
				 */
				addresses = geocoder.getFromLocation(location.getLatitude(),
						location.getLongitude(), 1);
				/*
				 * addresses = LocationUtils
				 * .getStringFromLocation(mlocation.getLatitude(),
				 * mlocation.getLongitude());
				 */

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
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					return exception1.getMessage();
				}

				// Return an error message
				// return (getString(R.string.IO_Exception_getFromLocation));

				// Catch incorrect mlatitude or mlongitude values
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
						"http://pideuntaxi.co/pideuntaxi/carreras/registrar");
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
			} catch (Exception e1) {
				Log.e("JSON Parser", "Error parsing data " + e1.toString());
			}

			// try parse the string to a JSON object
			try {
				Log.e("json entro", sb.toString());
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
						b.putString("ID", json.getString(KEY_ID));
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

	@Override
	public void onLocationChanged(Location location) {

	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

}

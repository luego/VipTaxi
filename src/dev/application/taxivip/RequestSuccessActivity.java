package dev.application.taxivip;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class RequestSuccessActivity extends ActionBarActivity {
	
	public static String placa_id = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register_success);
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		
		TextView txtAddress = (TextView)findViewById(R.id.txtAddressSent);
		 
        //Recuperamos la información pasada en el intent
        Bundle bundle = this.getIntent().getExtras();

        //Construimos el mensaje a mostrar
        //txtAddress.setText("Ya te enviamos tu taxi a la dirección " + bundle.getString("DIRECCION"));
        txtAddress.setText("Se envío su solicitud de servicio, consulte en breve para ver el estado.  ");
        placa_id = bundle.getString("ID");
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.register_success, menu);
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
	
	public void whoIsMyTaxi(View v){
		//NavUtils.navigateUpFromSameTask(this);
		Intent intent = new Intent(RequestSuccessActivity.this,
				WhoIsMyTaxiActivity.class);

		// Creamos la información a pasar entre actividades
		Bundle b = new Bundle();
		b.putString("ID",placa_id);
		// Añadimos la información al intent
		intent.putExtras(b);
		// Iniciamos la nueva actividad
		startActivity(intent);
	}
	
	
	

}

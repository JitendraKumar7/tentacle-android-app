package com.sunoray.tentacle.extraActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.R;
import com.sunoray.tentacle.common.AppProperties;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

public class MsgViewActivity extends Activity {
	
	private static final Logger log = LoggerFactory.getLogger(MsgViewActivity.class);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		try {
			Bundle extras = this.getIntent().getExtras();
			if (extras != null) {
				String showType = extras.getString("showType");
				String message = extras.getString("message");
				final String finalurl=extras.getString("finalurl");
				extras.clear();
				
				this.setTheme(R.style.AppTheme);
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
				if(showType==null){
					alertDialogBuilder.setTitle("Tentacle");
					alertDialogBuilder
						.setMessage(message)
						.setCancelable(false)
						.setPositiveButton("OK",new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,int id) {
								// Setting activeAlertDialog to false on OK
								AppProperties.activeAlertDialog = false;
								dialog.dismiss();
								finish();
							}
						});
				} else if(showType.equalsIgnoreCase("view_tel_alert")){
					
					/*alertDialogBuilder.setTitle("Tentacle")
						.setMessage("Loging of this call is not enabled \n Do you want to continue?")
						.setCancelable(false)
						.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,int id) {								
								dialog.dismiss();
								finish();*/
								Intent callIntent = new Intent(Intent.ACTION_DIAL,Uri.parse(finalurl));
								startActivity(callIntent);
							/*}
						}).setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								finish();
							}
						});		*/			
				}				
				AlertDialog alertDialog = alertDialogBuilder.create();
				// Setting activeAlertDialog to true before display
				AppProperties.activeAlertDialog = true;
				alertDialog.show();
			}	
		} catch (Exception e) {
			log.debug("Error in message alert:"+e.getMessage());
		}		
	}
}
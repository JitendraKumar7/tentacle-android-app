package com.sunoray.tentacle.layout;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.sunoray.tentacle.MainActivity;
import com.sunoray.tentacle.R;
import com.sunoray.tentacle.ViewActivity;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class InboundDialog extends Dialog implements View.OnClickListener {

    private static final Logger log = LoggerFactory.getLogger(InboundDialog.class);
    private String callerName = "";
    private Recording rec = null;
    private TextView txt_cancel, txt_phn_no, txt_save;
    private Context context;

    public InboundDialog(Context context, Recording rec) {
        super(context);
        this.context = context;
        this.rec = rec;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup);
        setCanceledOnTouchOutside(true);
        // setCancelable(false);
        try {
            txt_cancel = (TextView) findViewById(R.id.txt_cancel);
            txt_cancel.setOnClickListener(this);

            txt_save = (TextView) findViewById(R.id.txt_save);
            txt_save.setOnClickListener(this);

            txt_phn_no = (TextView) findViewById(R.id.txt_cell_no);

            List<String> names = getContactName(context, rec.getPhoneNumber());
            if (names.size() > 0)
                callerName = names.get(0);

            if (rec.getPhoneNumber() != null && !rec.getPhoneNumber().trim().isEmpty())
                if (callerName != null && !callerName.trim().isEmpty())
                    txt_phn_no.setText(callerName + "\n" + rec.getPhoneNumber());
                else
                    txt_phn_no.setText(rec.getPhoneNumber());

        } catch (Exception e) {
            log.debug("Exception: ", e);
        }
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.txt_cancel:
                    try {
                        log.debug("Save cancled. Deleting Recording");
                        new File(rec.getPath()).delete();
                        new DatabaseHandler(context).deleteRecording(rec.getId());
                        rec = null;
                        if (context instanceof MainActivity)
                            ((MainActivity) context).reloadView();
                    } catch (Exception e) {
                        log.debug("Exception: ",e);
                    }
                    this.dismiss();
                    break;
                case R.id.txt_save:
                    try {
                        if (rec.getId() != -1) {
                            Intent viewIntent = new Intent(context, ViewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            viewIntent.putExtra("goTo", "URL");
                            viewIntent.putExtra("action", AppProperties.SEND_INBOUND_CALL + "?phone_number=" + URLEncoder.encode(rec.getPhoneNumber(), "UTF-8") + "&log_uid=" + rec.getId());
                            context.startActivity(viewIntent);
                        } else
                            log.debug("ERROR NOT ABLE TO STORE IN DB");
                    } catch (Exception e) {
                        log.debug("Exception: ", e);
                    } finally {
                        //AsyncTask<Recording, Void, String> aTask = new AddRecording(context);
                        //aTask.execute(rec);
                    }
                    this.dismiss();
                    break;
            }
        } catch (Exception e) {
            log.debug("Exception: ", e);
        }
    }

    private List<String> getContactName(Context context, String phoneNumber) {
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.PhoneLookup._ID}, null, null, null);
            List<String> idList = new ArrayList<>();
            List<String> nameList = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
                    if (!idList.contains(contactId))
                        idList.add(contactId); //adding unique id to list
                }
                //pass unique ids to get contact names
                for (int i = 0; i < idList.size(); i++) {
                    //String tempId = idList.get(i);
                    Cursor cursorDetails = context.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{idList.get(i)}, null);
                    if (cursorDetails != null) {
                        if (cursorDetails.moveToFirst()) {
                            String contactName = cursorDetails.getString(cursorDetails.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                            if (!nameList.contains(contactName))
                                nameList.add(contactName);
                        }
                        cursorDetails.close();
                    }
                }
                cursor.close();
            }
            return nameList;
        } catch (Exception e) {
            log.debug("Exception in getContactName(): ", e);
        }
        return null;
    }
}
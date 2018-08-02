package com.sunoray.tentacle.helper;

import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;

public class ImageProcessor {

	private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);
	
	public static void Rescale(Uri fileUri,Uri newfile) {

		FileOutputStream ostream = null;
		try {
			Options mBitmapOptions = new Options();
			mBitmapOptions.inJustDecodeBounds = true;
			mBitmapOptions.inSampleSize = 5;
			Bitmap image = BitmapFactory.decodeFile(fileUri.getPath(), mBitmapOptions);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 80;

			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (mBitmapOptions.outWidth/scale/2 >= REQUIRED_SIZE && mBitmapOptions.outHeight/scale/2 >= REQUIRED_SIZE) {
				scale *= 2;
			}

			BitmapFactory.Options options2 = new BitmapFactory.Options();
			options2.inSampleSize = scale;
			image = BitmapFactory.decodeFile(fileUri.getPath(), options2);
			
			//Write compressed file		
			log.info("Path of new file=" + newfile.getPath()  + " | old file=" + fileUri.getPath());
			File file = new File(newfile.getPath());
			ostream = new FileOutputStream(file);
			
			image.compress(CompressFormat.JPEG, 80, ostream);
			
			log.debug("Image Compressed. final size is: " + file.length()/1024 + "KB");
		} catch (Exception e) {
			log.info("Exception in rescale: " , e);
		} finally {
			try {
				ostream.close();
			} catch (Exception e) {}
		}
	}
	
}

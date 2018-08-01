package com.sunoray.tentacle.helper;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import com.sunoray.tentacle.common.AppProperties;

public class StorageHandler {
	private static final Logger log = LoggerFactory.getLogger(StorageHandler.class);
	/*It is specified which Storage available for use*/
	public static final String EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
	public static final String INTERNAL_STORAGE = "INTERNAL_STORAGE";
	public static final String NO_STORAGE = "NO_STORAGE";	

	public static String getFilename(String fileName, String fileExtention) {
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath, AppProperties.DEVICE_RECORDING_PATH);
		if (!file.exists()) {
			log.info("=" + file.mkdirs());
		}
		String fullPath = file.getAbsolutePath() + "/" + fileName+ fileExtention;
		log.info("Rec FullPath: " + fullPath);
		return (fullPath);
	}

	/*Returns DIR path which is available*/
	public static File getFileDirPath(Context context, String Path) {
		File imageStorageDir = null;
		if (StorageHandler.setStorageAvailable().equalsIgnoreCase(StorageHandler.EXTERNAL_STORAGE)) {
			imageStorageDir = new File(Environment.getExternalStorageDirectory().getPath(), Path);
			if (!imageStorageDir.exists()) {
				imageStorageDir.mkdirs();
			}
		} else if (StorageHandler.setStorageAvailable().equalsIgnoreCase(StorageHandler.INTERNAL_STORAGE)) {
			imageStorageDir = context.getFilesDir();
			if (!imageStorageDir.exists()) {
				imageStorageDir.mkdirs();
			}
		}
		return imageStorageDir;
	}

	public void saveFile() {
	}

	public void readFile() {
	}

	public void removeFile() {
	}

	public static String setStorageAvailable() {

		if (externalMemoryAvailable())
			return EXTERNAL_STORAGE;
		else if (getAvailableInternalMemorySize() > 10)
			return INTERNAL_STORAGE;
		else
			return NO_STORAGE;
	}

	public boolean checkStorageAvaibility() {
		return (externalMemoryAvailable() || getAvailableInternalMemorySize() > 10) ? true
				: false;
	}

	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static long getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = android.os.Build.VERSION.SDK_INT < 18 ? stat.getBlockSize() : stat.getFreeBlocksLong();
		long availableBlocks = android.os.Build.VERSION.SDK_INT < 18 ? stat.getAvailableBlocks() : stat.getAvailableBlocksLong();
		return (availableBlocks * blockSize);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static long getAvailableExternalMemorySize() {
		if (externalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = android.os.Build.VERSION.SDK_INT < 18 ? stat.getBlockSize() : stat.getFreeBlocksLong();
			long availableBlocks = android.os.Build.VERSION.SDK_INT < 18 ? stat.getAvailableBlocks() : stat.getAvailableBlocksLong();
			return (availableBlocks * blockSize);
		} else {
			return 0;
		}
	}

	private static boolean externalMemoryAvailable() {
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	@SuppressWarnings("unused")
	private static String formatSize(long size) {
		String suffix = "MB";
		size /= (1024 * 1024);

		StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

		int commaOffset = resultBuffer.length() - 3;
		while (commaOffset > 0) {
			resultBuffer.insert(commaOffset, ',');
			commaOffset -= 3;
		}

		if (suffix != null)
			resultBuffer.append(suffix);
		return resultBuffer.toString();
	}
}

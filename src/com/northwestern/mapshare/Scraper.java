package com.northwestern.mapshare;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;

public class Scraper {
	public static final String IMAGE_FOLDER = "mapshare_img";
	public static void scrapeScreen(View screen, String fname) {
		// image naming and path  to include sd card  appending name you choose for file
		String mPath = Environment.getExternalStorageDirectory().toString() + "/" + IMAGE_FOLDER + fname + ".png";   

		Process process;
		try {
			process = Runtime.getRuntime().exec("su");
	    DataOutputStream os = new DataOutputStream(process.getOutputStream());

	    os.writeBytes("/system/bin/screencap -p " + mPath);
	    os.writeBytes("exit\n");
	    os.flush();
	    os.close();

	    process.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

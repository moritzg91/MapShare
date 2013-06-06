package com.northwestern.mapshare;

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
		String mPath = Environment.getExternalStorageDirectory().toString() + "/" + IMAGE_FOLDER + fname;   

		// create bitmap screen capture
		Bitmap bitmap;
		View v1 = screen.getRootView();
		v1.setDrawingCacheEnabled(true);
		bitmap = Bitmap.createBitmap(v1.getDrawingCache());
		v1.setDrawingCacheEnabled(false);

		OutputStream fout = null;
		File imageFile = new File(mPath);

		try {
			fout = new FileOutputStream(imageFile);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
			fout.flush();
			fout.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

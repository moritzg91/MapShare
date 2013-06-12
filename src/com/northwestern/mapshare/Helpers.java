package com.northwestern.mapshare;

import java.util.Comparator;
import java.util.List;

import com.northwestern.mapshare.BroadcastManager.Result;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Helpers {
	
	// combines multiple bitmaps into a single one, putting up to "numX" bmps next to each other and up to "numY" bmps on top of each other
	public static Bitmap combineBitmaps(List<Bitmap> bmps, int numX, int numY) {
		int pxWidth = bmps.get(0).getWidth()*numX;
		int pxHeight = bmps.get(0).getHeight()*numY;
		
		Bitmap joinedBmp = Bitmap.createBitmap(pxWidth, pxHeight, Bitmap.Config.ARGB_8888);
		Canvas comboImg = new Canvas(joinedBmp);
		
		int posX = 0;
		int curXcount = 0;
		int posY = 0;
		
		for (Bitmap bmp : bmps) {
			comboImg.drawBitmap(bmp, posX, posY, null);
			posX += bmp.getWidth();
			curXcount += 1;
			if (curXcount > numX) {
				curXcount = 0;
				posX = 0;
				posY += bmp.getHeight();
			}
		}
		return joinedBmp;
	}

	public class ResultComparator implements Comparator<Result> {
	    @Override
	    public int compare(Result r1, Result r2) {
	        if (r1.topLeft.longitude > r2.topLeft.longitude) {
	        	return -1;
	        } else if (r1.topLeft.longitude == r2.topLeft.longitude) {
	        	if (r1.topLeft.latitude < r2.topLeft.latitude) {
		        	return -1;
		        } else if (r1.topLeft.latitude > r2.topLeft.latitude) {
		        	return 1;
		        } else {
		        	return 0;
		        }
	        }
	        return 1;
	    }
	}
}

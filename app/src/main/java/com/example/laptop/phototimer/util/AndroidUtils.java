package com.example.laptop.phototimer.util;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.view.View;

public class AndroidUtils {

	public static void bindOnClickListener(final Object target, View view, String methodName) {
		final Method method;
		try {
			method = target.getClass().getMethod(methodName);
		}
		catch(Exception ex) {
			throw new IllegalArgumentException(ex);
		}
		view.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					method.invoke(target);
				}
				catch(Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}

	public static interface MediaScannerCallback {
		public void mediaScannerCompleted(String scanPath, Uri scanURI);
	}

    public static void scanSavedMediaFile(final Context context, final String path, final MediaScannerCallback callback) {
    	final MediaScannerConnection[] scannerConnection = new MediaScannerConnection[1];
		try {
			MediaScannerConnection.MediaScannerConnectionClient scannerClient = new MediaScannerConnection.MediaScannerConnectionClient() {
				public void onMediaScannerConnected() {
					scannerConnection[0].scanFile(path, null);
				}

				public void onScanCompleted(String scanPath, Uri scanURI) {
					scannerConnection[0].disconnect();
					if (callback!=null) {
						callback.mediaScannerCompleted(scanPath, scanURI);
					}
				}
			};
    		scannerConnection[0] = new MediaScannerConnection(context, scannerClient);
    		scannerConnection[0].connect();
		}
		catch(Exception ignored) {}
    }
    
    public static void scanSavedMediaFile(final Context context, final String path) {
    	scanSavedMediaFile(context, path, null);
    }
    
	public static BitmapFactory.Options computeBitmapSizeFromURI(Context context, Uri imageURI) throws FileNotFoundException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageURI), null, options);
		return options;
	}
	
	public static Bitmap scaledBitmapFromURIWithMinimumSize(Context context, Uri imageURI, int width, int height) throws FileNotFoundException {
		BitmapFactory.Options options = computeBitmapSizeFromURI(context, imageURI);
		options.inJustDecodeBounds = false;
		
		float wratio = 1.0f*options.outWidth / width;
		float hratio = 1.0f*options.outHeight / height;		
		options.inSampleSize = (int)Math.min(wratio, hratio);
		
		return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageURI), null, options);		
	}
	
	public static void getScaledWidthAndHeightToMaximum(
			int width, int height, int maxWidth, int maxHeight, int[] output) {
		output[0] = width;
		output[1] = height;

		if (width==maxWidth && height<=maxHeight) return;
		if (height==maxHeight && width<=maxWidth) return;
		float wratio = ((float)width)/maxWidth;
		float hratio = ((float)height)/maxHeight;
		if (wratio<=hratio) {

			output[0] = (int)(width/hratio);
			output[1] = maxHeight;
		}
		else {

			output[0] = maxWidth;
			output[1] = (int)(height/wratio);
		}
	}
	
	public static int[] scaledWidthAndHeightToMaximum(int width, int height, int maxWidth, int maxHeight) {
		int[] output = new int[2];
		getScaledWidthAndHeightToMaximum(width, height, maxWidth, maxHeight, output);
		return output;
	}

    public static boolean setSystemUiLowProfile(View view) {
        return setSystemUiVisibility(view, "SYSTEM_UI_FLAG_LOW_PROFILE");
    }
    
    static boolean setSystemUiVisibility(View view, String flagName) {
        try {
            Method setUiMethod = View.class.getMethod("setSystemUiVisibility", int.class);
            Field flagField = View.class.getField(flagName);
            setUiMethod.invoke(view, flagField.get(null));
            return true;
        }
        catch(Exception ex) {
            return false;
        }
    }
    
    public static int getBitmapByteCount(Bitmap bitmap) {
        try {
            Method byteCountMethod = Bitmap.class.getMethod("getByteCount");
            return (Integer)byteCountMethod.invoke(bitmap);
        }
        catch(Exception ex) {
            return 4 * bitmap.getWidth() * bitmap.getHeight();
        }
    }
}

package com.example.laptop.phototimer.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.support.v4.util.LruCache;


public class ScaledBitmapCache {

    static int MEMORY_CACHE_SIZE = 2*1024*1024;
	
	public static interface ThumbnailLocator {
		File thumbnailFileForUri(Uri imageUri);
	}

	public static ThumbnailLocator createFixedDirectoryLocator(final String thumbnailDirectory) {
	    return new ThumbnailLocator() {
	        public File thumbnailFileForUri(Uri imageUri) {
	            String filename = imageUri.getLastPathSegment();
	            return new File(thumbnailDirectory + File.separator + filename);
	        }   
	    };
	}

	Context context;
	ThumbnailLocator thumbnailLocator;
	
	LruCache<Uri, Bitmap> scaledBitmapCache = new LruCache<Uri, Bitmap>(MEMORY_CACHE_SIZE) {
	    @Override protected int sizeOf(Uri uri, Bitmap bitmap) {
	        int size = AndroidUtils.getBitmapByteCount(bitmap);
	        return size;
	    }
	};
	
	public ScaledBitmapCache(Context context, ThumbnailLocator thumbnailLocator) {
		this.context = context;
		this.thumbnailLocator = thumbnailLocator;
	}
	
	public ScaledBitmapCache(Context context, String imageDirectory) {
		this(context, createFixedDirectoryLocator(imageDirectory));
	}
	
	public Bitmap getInMemoryScaledBitmap(Uri imageUri, int minWidth, int minHeight) {
        Bitmap bitmap = scaledBitmapCache.get(imageUri);
        if (bitmap!=null && bitmap.getWidth()>=minWidth && bitmap.getHeight()>=minHeight) {
            return bitmap;
        }
        return null;
	}
	
	public Bitmap getScaledBitmap(Uri imageUri, int minWidth, int minHeight) {
		Bitmap bitmap = getInMemoryScaledBitmap(imageUri, minWidth, minHeight);
		if (bitmap!=null) return bitmap;

		File thumbfile = thumbnailLocator.thumbnailFileForUri(imageUri);
		if (thumbfile!=null && thumbfile.isFile()) {
			try {
				bitmap = AndroidUtils.scaledBitmapFromURIWithMinimumSize(context, 
						Uri.fromFile(thumbfile), minWidth, minHeight);
				if (bitmap!=null && bitmap.getWidth()>=minWidth && bitmap.getHeight()>=minHeight) {
					scaledBitmapCache.put(imageUri, bitmap);
					return bitmap;
				}
			}
			catch(Exception ignored) {}
		}

		try {
			bitmap = AndroidUtils.scaledBitmapFromURIWithMinimumSize(context, imageUri, minWidth, minHeight);
		}
		catch(Exception ex) {
			bitmap = null;
		}
		if (bitmap!=null) {
			scaledBitmapCache.put(imageUri, bitmap);
			try {
				thumbfile.getParentFile().mkdirs();
				OutputStream thumbnailOutputStream = new FileOutputStream(thumbfile);
				bitmap.compress(CompressFormat.JPEG, 90, thumbnailOutputStream);
				thumbnailOutputStream.close();
				(new File(thumbfile.getParentFile().getPath() + File.separator + ".nomedia")).createNewFile();
			}
			catch(Exception ignored) {}
		}
		return bitmap;
	}
	
	public void removeUri(Uri imageUri) {
		scaledBitmapCache.remove(imageUri);
		thumbnailLocator.thumbnailFileForUri(imageUri).delete();
	}
}

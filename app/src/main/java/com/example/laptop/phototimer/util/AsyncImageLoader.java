package com.example.laptop.phototimer.util;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

public class AsyncImageLoader {
    
    static class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
        WeakReference<ImageView> imageViewReference;
        Uri data = null;
        ScaledBitmapCache bitmapCache;
        int width;
        int height;

        public BitmapWorkerTask(ImageView imageView, ScaledBitmapCache bitmapCache, int width, int height) {
            imageViewReference = new WeakReference(imageView);
            this.bitmapCache = bitmapCache;
            this.width = width;
            this.height = height;
        }

        @Override protected Bitmap doInBackground(Uri...args) {
            data = args[0];
            return bitmapCache.getScaledBitmap(args[0], width, height);
        }

        @Override protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
        
        private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
            if (imageView != null) {
                final Drawable drawable = imageView.getDrawable();
                if (drawable instanceof AsyncDrawable) {
                    final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                    return asyncDrawable.getBitmapWorkerTask();
                }
            }
            return null;
        }

        public static boolean cancelPotentialWork(Uri uri, ImageView imageView) {
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (bitmapWorkerTask != null) {
                final Uri taskUri = bitmapWorkerTask.data;
                if (!uri.equals(taskUri)) {
                    bitmapWorkerTask.cancel(true);
                } 
                else {
                    return false;
                }
            }
            return true;
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, AsyncTask bitmapWorkerTask) {
            super(res, (Bitmap)null);
            bitmapWorkerTaskReference = new WeakReference(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public void loadImageIntoViewAsync(final ScaledBitmapCache bitmapCache, Uri imageUri, ImageView imageView,
            final int width, final int height, Resources resources) {
        Bitmap bitmap = bitmapCache.getInMemoryScaledBitmap(imageUri, width, height);
        if (bitmap!=null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        if (BitmapWorkerTask.cancelPotentialWork(imageUri, imageView)) {
            BitmapWorkerTask task = new BitmapWorkerTask(imageView, bitmapCache, width, height);
            AsyncDrawable asyncDrawable = new AsyncDrawable(resources, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(imageUri);
        }
    }

}

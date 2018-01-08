package com.example.laptop.phototimer.PhotoTimer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.widget.ImageView;

import com.example.laptop.phototimer.util.AndroidUtils;

import java.io.File;

public class ViewImageActivity extends Activity {

    public static final int DELETE_RESULT = Activity.RESULT_FIRST_USER;

    ImageView imageView;
    Uri imageUri;

    public static Intent startActivityWithImageURI(Activity parent, Uri imageURI, String type) {
        Intent intent = new Intent(parent, ViewImageActivity.class);
        intent.setDataAndType(imageURI, type);
        parent.startActivityForResult(intent, 0);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.imageview);

        imageView = (ImageView)findViewById(R.id.imageView);
        imageUri = getIntent().getData();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        try {
            imageView.setImageBitmap(AndroidUtils.scaledBitmapFromURIWithMinimumSize(this, imageUri,
                    dm.widthPixels, dm.heightPixels));
        }
        catch(Exception ex) {}

        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.deleteImageButton), "deleteImage");
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.shareImageButton), "shareImage");
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.exitViewImageButton), "goBack");
    }

    public void goBack() {
        this.finish();
    }

    public void deleteImage() {
        String path = this.getIntent().getData().getPath();
        (new File(path)).delete();
        this.setResult(DELETE_RESULT);
        this.finish();
    }

    public void shareImage() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(this.getIntent().getType());
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Picture Using:"));
    }

    public void viewImageInGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
        galleryIntent.setDataAndType(this.getIntent().getData(), this.getIntent().getType());
        galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        this.startActivity(galleryIntent);
        this.finish();
    }

}

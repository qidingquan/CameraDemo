package com.example.qdq.camera;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageView;

public class PhotoShowActivity extends AppCompatActivity {
    private ImageView iv_show;
    private Uri url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_photo_show);
        url=getIntent().getParcelableExtra("url");

        iv_show= (ImageView) findViewById(R.id.iv_show);
        iv_show.setImageURI(url);
    }
}

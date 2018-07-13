package hk.haowei.wifi.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.R;


public class ImageViewerActivity extends AppCompatActivity implements View.OnClickListener {

    protected ImageView imageView;
    protected RelativeLayout mainView;


    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);

        initView();

    }

    protected void initView() {
        mainView = findViewById(R.id.mainView);
        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(AAplication.instance.get().userAvatar);

        mainView.setClickable(true);
        mainView.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        finish();
    }
}

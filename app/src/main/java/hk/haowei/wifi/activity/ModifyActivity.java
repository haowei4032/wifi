package hk.haowei.wifi.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.R;

public class ModifyActivity extends AppCompatActivity {

    protected int index = -1;
    protected String field = null;
    protected EditText modifyValue;

    //protected String saveValue = "";

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify);

        initView();

    }


    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.login_title);

        TextView firstButton = findViewById(R.id.firstButton);
        firstButton.setClickable(true);
        firstButton.setText(R.string.first_button);
        firstButton.setTypeface(Typeface.createFromAsset(getAssets(), "apps.ttf"));

        firstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        modifyValue = findViewById(R.id.modifyValue);

        Intent intent = getIntent();
        index = intent.getIntExtra("index", -1);
        field = intent.getStringExtra("field");
        titleView.setText(intent.getStringExtra("title"));

        if (intent.getStringExtra("descript") != null) {
            TextView descriptView = findViewById(R.id.modifyDescript);
            descriptView.setText(intent.getStringExtra("descript"));
        }

        if (intent.getStringExtra("defaultValue") != null) {
            modifyValue.setText(intent.getStringExtra("defaultValue"));
            modifyValue.setSelection(modifyValue.length());
        }

    }

    public void onBackPressed() {
        Intent intent = getIntent();
        intent.putExtra("field", field);
        intent.putExtra("index", index);
        intent.putExtra("value", modifyValue.getText().toString().trim());
        setResult(RESULT_OK, intent);
        try {
            if (AAplication.instance.get().userInfo.opt(field) != null) {
                AAplication.instance.get().userInfo.put(field, modifyValue.getText().toString().trim());
            }
        }catch (Exception e) {
            Log.e("MODIFY.CACHE.ERROR", e.getMessage());
        }
        super.onBackPressed();
    }
}

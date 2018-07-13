package hk.haowei.wifi.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import hk.haowei.wifi.R;
import hk.haowei.wifi.view.ListViewEx;

public class MessageActivity extends AppCompatActivity {

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initView();
    }

    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.message_title);

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

        LinearLayout listContainer = findViewById(R.id.listContainer);
        final ListViewEx listView = new ListViewEx(getContext());
        listView.setDividerHeight(1);
        listView.setBackgroundColor(Color.WHITE);
        listView.setAdapter(new DataAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("title", "系统消息");
                intent.putExtra("userid", 1);
                startActivity(intent);
            }
        });
        listContainer.addView(listView);

    }

    protected class DataAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.message_list_item, null);

            return convertView;
        }


    }

}

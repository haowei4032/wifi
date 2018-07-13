package hk.haowei.wifi.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.amap.api.location.CoordinateConverter;
import com.amap.api.location.DPoint;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;

import hk.haowei.wifi.R;

public class MapActivity extends AppCompatActivity {

    protected AMap aMap;

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        MapView mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        initView();
    }

    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.map_title);

        TextView firstButton = findViewById(R.id.firstButton);
        firstButton.setClickable(true);
        firstButton.setText(R.string.first_button);
        firstButton.setTypeface(Typeface.createFromAsset(getAssets(), "apps.ttf"));
        firstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        onPosition();
    }

    protected void onPosition() {
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context
                        .LOCATION_SERVICE);
            try {
                Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));


                CoordinateConverter converter = new CoordinateConverter(getContext());
                converter.from(CoordinateConverter.CoordType.GPS);
                converter.coord(new DPoint(location.getLatitude(), location.getLongitude()));
                DPoint desLatLng = converter.convert();


                LatLng point = new LatLng(desLatLng.getLatitude(), desLatLng.getLongitude());
                MarkerOptions options = new MarkerOptions().position(point);
                aMap.addMarker(options);
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(point));
                aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
            }catch (Exception e) {
                Log.e("LOCATION", e.getMessage());
            }
        }
    }

}

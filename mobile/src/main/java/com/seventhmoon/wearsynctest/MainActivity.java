package com.seventhmoon.wearsynctest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener  {
    private static final String TAG = MainActivity.class.getName();
    private GoogleApiClient mGoogleApiClient;
    private int mTextColor = 0xffffffff;
    Bitmap mBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        Button buttonOK = (Button)findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup radioTextColor = (RadioGroup) findViewById(R.id.radioTextColor);
                int selectedId = radioTextColor.getCheckedRadioButtonId();
                switch (selectedId) {
                    default:
                    case R.id.radio_tc1:
                        mTextColor = 0xffffffff;
                        break;
                    case R.id.radio_tc2:
                        mTextColor = 0xff00ff00;
                        break;
                    case R.id.radio_tc3:
                        mTextColor = 0xffffff00;
                        break;
                    case R.id.radio_tc4:
                        mTextColor = 0xff00ffff;
                        break;
                    case R.id.radio_tc5:
                        mTextColor = 0xffff00ff;
                        break;
                }

                sendParamsAndFinish();
            }
        });

        Button buttonCancel = (Button)findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendParamsAndFinish();
            }
        });

        Button buttonONewPic = (Button)findViewById(R.id.buttonONewPic);
        buttonONewPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup radioTextColor = (RadioGroup) findViewById(R.id.radioNewPic);
                int selectedId = radioTextColor.getCheckedRadioButtonId();

                sendAssetAndFinish(selectedId);
            }
        });
    }

    // send Asset through Google API
    private void sendAssetAndFinish(int id) {
        // create an Asset
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                ((id == R.id.radio_np1) ? R.drawable.cb11 : R.drawable.cb12)), 320, 320, false);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        Asset asset = Asset.createFromBytes(baos.toByteArray());

        // send Asset
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/newpic");
        putDataMapReq.getDataMap().putAsset("assetbody", asset);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, putDataReq);

        finish();
    }

    // sends data through Google API
    private void sendParamsAndFinish() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/watch_face_config_cliu");
        putDataMapReq.getDataMap().putInt("text_color", mTextColor);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

}

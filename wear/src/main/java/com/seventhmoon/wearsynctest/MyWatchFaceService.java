package com.seventhmoon.wearsynctest;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MyWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = MyWatchFaceService.class.getName();

    private static final long UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        Paint mDigitalPaint;
        Paint mDigitalPaintOuter;
        boolean mMute;
        Time mTime;
        private GoogleApiClient mGoogleApiClient;

        static final int MESSAGE_ID_UPDATE_TIME = 1000;
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MESSAGE_ID_UPDATE_TIME:
                        invalidate();
                        if (isVisible() && !isInAmbientMode()) {
                            long delay = UPDATE_INTERVAL
                                    - (System.currentTimeMillis() % UPDATE_INTERVAL);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MESSAGE_ID_UPDATE_TIME, delay);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        private int mTextSize = 24;
        private float mStrokeWidth = 32.0f;
        private int mTextColor = 0xffffffff;
        private int mTextColorOuter = 0xff000000;
        private float offsetx = (float)(-50 + 100 * Math.random());
        private float offsety = (float)(-50 + 100 * Math.random());

        boolean mLowBitAmbient = false;
        final int [] mBackgroundIDs = {R.drawable.cb4, R.drawable.cb5, R.drawable.cb6};
        Bitmap mBG;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            // randomly pick one of the three photos by Chris Blunt
            mBG = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                    mBackgroundIDs[(int)(mBackgroundIDs.length * Math.random())]), 320, 320, false);

            mDigitalPaint = new Paint();
            mDigitalPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mDigitalPaint.setTextSize(mTextSize);
            mDigitalPaint.setStrokeWidth(mStrokeWidth);
            mDigitalPaint.setColor(mTextColor);
            mDigitalPaint.setStyle(Paint.Style.FILL);
            mDigitalPaint.setAntiAlias(true);

            mDigitalPaintOuter = new Paint();
            mDigitalPaintOuter.setTypeface(Typeface.DEFAULT_BOLD);
            mDigitalPaintOuter.setTextSize(mTextSize);
            mDigitalPaintOuter.setStrokeWidth(mStrokeWidth);
            mDigitalPaintOuter.setColor(mTextColorOuter);
            mDigitalPaintOuter.setStyle(Paint.Style.FILL);
            mDigitalPaintOuter.setAntiAlias(true);

            mTime = new Time();

            mGoogleApiClient = new GoogleApiClient.Builder(MyWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mLowBitAmbient) {
                mDigitalPaint.setAntiAlias(!inAmbientMode);
                mDigitalPaintOuter.setAntiAlias(!inAmbientMode);
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean inMuteMode = (interruptionFilter == MyWatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mDigitalPaint.setAlpha(inMuteMode ? 80 : 255);
                mDigitalPaintOuter.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // draw the background image
            if (mBG == null || mBG.getWidth() != bounds.width() || mBG.getHeight() != bounds.height())
                mBG = Bitmap.createScaledBitmap(mBG, bounds.width(), bounds.height(), false);
            canvas.drawBitmap(mBG, 0, 0, null);

            // draw the time
            String ts1 = String.format("%02d:%02d:%02d %s",
                    (mTime.hour == 12) ? 12 : (mTime.hour % 12), mTime.minute, mTime.second,
                    (mTime.hour < 12) ? "am" : "pm");
            float tw1 = mDigitalPaint.measureText(ts1);
            float tx1 = (bounds.width() - tw1) / 2 + offsetx;
            float ty1 = bounds.height() / 2 + offsety;
            canvas.drawText(ts1, tx1 - 1, ty1 - 1, mDigitalPaintOuter);
            canvas.drawText(ts1, tx1 + 1, ty1 - 1, mDigitalPaintOuter);
            canvas.drawText(ts1, tx1 - 1, ty1 + 1, mDigitalPaintOuter);
            canvas.drawText(ts1, tx1 + 1, ty1 + 1, mDigitalPaintOuter);
            canvas.drawText(ts1, tx1, ty1, mDigitalPaint);

            // draw the date
            String ts2 = String.format("%02d/%02d/%04d", mTime.month + 1, mTime.monthDay, mTime.year);
            float tw2 = mDigitalPaint.measureText(ts2);
            float tx2 = (bounds.width() - tw2) / 2 + offsetx;
            float ty2 = ty1 + mTextSize + 5;
            canvas.drawText(ts2, tx2 - 1, ty2 - 1, mDigitalPaintOuter);
            canvas.drawText(ts2, tx2 + 1, ty2 - 1, mDigitalPaintOuter);
            canvas.drawText(ts2, tx2 - 1, ty2 + 1, mDigitalPaintOuter);
            canvas.drawText(ts2, tx2 + 1, ty2 + 1, mDigitalPaintOuter);
            canvas.drawText(ts2, tx2, ty2, mDigitalPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                mGoogleApiClient.connect();
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
                releaseGoogleApiClient();
            }

            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver)
                return;

            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver)
                return;

            mRegisteredTimeZoneReceiver = false;
            MyWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MESSAGE_ID_UPDATE_TIME);

            if (isVisible() && !isInAmbientMode())
                mUpdateTimeHandler.sendEmptyMessage(MESSAGE_ID_UPDATE_TIME);
        }

        private void releaseGoogleApiClient() {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(mGoogleApiClient, onDataChangedListener);
                mGoogleApiClient.disconnect();
            }
        }

        @Override
        public void onConnected(Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, onDataChangedListener);
            Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);
        }

        private void updateAssetForDataItem(DataItem item) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

            if (dataMap.containsKey("assetbody")) {
                Asset asset = dataMap.getAsset("assetbody");

                if (asset == null)
                    return;

                ConnectionResult cr = mGoogleApiClient.blockingConnect(5000, TimeUnit.MILLISECONDS);
                if (!cr.isSuccess())
                    return;

                InputStream is = Wearable.DataApi.getFdForAsset(
                        mGoogleApiClient, asset).await().getInputStream();
                mGoogleApiClient.disconnect();

                if (is == null)
                    return;

                if (mBG != null) {
                    mBG.recycle();
                    mBG = null;
                }
                mBG = BitmapFactory.decodeStream(is);

                invalidate();
            }
        }

        private void updateParamsForDataItem(DataItem item) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

            if (dataMap.containsKey("text_color")) {
                int tc = dataMap.getInt("text_color");
                mDigitalPaint.setColor(tc);
                invalidate();
            }
        }

        private final DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEvents) {
                for (DataEvent event : dataEvents) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        if (item.getUri().getPath().equals("/newpic")) {
                            updateAssetForDataItem(item);
                        } else if ((item.getUri().getPath()).equals("/watch_face_config_cliu")) {
                            updateParamsForDataItem(item);
                        }
                    }
                }

                dataEvents.release();
                if (isVisible() && !isInAmbientMode()) {
                    invalidate();
                }
            }
        };

        private final ResultCallback<DataItemBuffer> onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    updateParamsForDataItem(item);
                }

                dataItems.release();
                if (isVisible() && !isInAmbientMode()) {
                    invalidate();
                }
            }
        };

        @Override
        public void onConnectionSuspended(int i) {
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MESSAGE_ID_UPDATE_TIME);
            releaseGoogleApiClient();
            super.onDestroy();
        }

    }
}

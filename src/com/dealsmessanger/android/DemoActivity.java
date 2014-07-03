/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dealsmessanger.android;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Main UI for the demo app.
 */
@SuppressLint("NewApi") public class DemoActivity extends Activity {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "328753786313";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM Demo";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;

    String regid;

	private LocationManager locationManager;
	private String provider;
	private MyLocationListener mylistener;
	private Criteria criteria;
	private Location location;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mDisplay = (TextView) findViewById(R.id.display);

        context = getApplicationContext();

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
//        if (checkPlayServices()) {
//            gcm = GoogleCloudMessaging.getInstance(this);
//            regid = getRegistrationId(context);
//
//            if (regid.isEmpty()) {
//                registerInBackground();
//            }
//        } else {
//            Log.i(TAG, "No valid Google Play Services APK found.");
//        }
        registerInBackground();
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the location provider
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); // default

		criteria.setCostAllowed(false);
		// get the best provider depending on the criteria
		provider = locationManager.getBestProvider(criteria, false);

		// the last known location of this provider
		location = locationManager.getLastKnownLocation(provider);

		mylistener = new MyLocationListener();

		if (location != null) {
			mylistener.onLocationChanged(location);
		} else {
			// leads to the settings because there is no last known location
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(intent);
		}
		// location updates: at least 1 meter and 200millsecs change
		locationManager.requestLocationUpdates(provider, 200, 1, mylistener);

    }

	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			new LocationSender().execute(new String[2]);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private class LocationSender extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

			JSONObject jsonObj = new JSONObject();
			JSONArray array = new JSONArray();

			try {

				final DeviceIdentifier deviceIdenifier = (DeviceIdentifier) getApplicationContext();

				array.put(location.getLatitude());
				array.put(location.getLongitude());

				if (deviceIdenifier.getDeviceId() != null) {
					jsonObj.put("deviceId", deviceIdenifier.getDeviceId());
				}
				jsonObj.put("gcmRegId", deviceIdenifier.getGcmRegId());
				// jsonObj.put("version", android.os.);
				jsonObj.put("platform", "ANDROID");
				jsonObj.put("platformVersion", android.os.Build.VERSION.RELEASE);
				// jsonObj.put("uniqueDeviceId", );
				jsonObj.put("brand", android.os.Build.BRAND);
				jsonObj.put("model", android.os.Build.MODEL);
				jsonObj.put("location", array);

				HttpClient httpClient = new DefaultHttpClient();
				HttpPost post = new HttpPost(
						"http://192.168.0.10:8080/dealsmessanger/device");
				// "http://127.0.0.1:8080/dealsmessanger/device");
				post.setHeader("content-type",
						"application/json; charset=UTF-8");

				StringEntity entity = new StringEntity(jsonObj.toString());

				post.setEntity(entity);
				HttpResponse httpResponse = httpClient.execute(post);
				String response = EntityUtils
						.toString(httpResponse.getEntity());

				if (response != null && response != "") {
					try {
						JSONObject resObj = new JSONObject(response);
						deviceIdenifier.setDeviceId(resObj
								.getString("deviceId"));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				System.out
						.println("server called------------->>>>>>>>>>>>>>>>");

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

	}

    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
//        final SharedPreferences prefs = getGcmPreferences(context);
//        int appVersion = getAppVersion(context);
//        Log.i(TAG, "Saving regId on app version " + appVersion);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putString(PROPERTY_REG_ID, regId);
//        editor.putInt(PROPERTY_APP_VERSION, appVersion);
//        editor.commit();
    	
        final DeviceIdentifier deviceIdenifier = (DeviceIdentifier) getApplicationContext();
        deviceIdenifier.setGcmRegId(regid);
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
//        final SharedPreferences prefs = getGcmPreferences(context);
//        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
//        if (registrationId.isEmpty()) {
//            Log.i(TAG, "Registration not found.");
//            return "";
//        }
//        // Check if app was updated; if so, it must clear the registration ID
//        // since the existing regID is not guaranteed to work with the new
//        // app version.
//        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
//        int currentVersion = getAppVersion(context);
//        if (registeredVersion != currentVersion) {
//            Log.i(TAG, "App version changed.");
//            return "";
//        }
        final DeviceIdentifier deviceIdenifier = (DeviceIdentifier) getApplicationContext();
        
        return deviceIdenifier.getGcmRegId();
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging
								.getInstance(getApplicationContext());
					}
					regid = gcm.register(SENDER_ID);
					msg = "Device registered, registration ID=" + regid;
					Log.i("GCM", msg);

				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();

				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				//etRegId.setText(msg + "\n");
				DeviceIdentifier deviceIdenifier = (DeviceIdentifier) getApplicationContext();
				deviceIdenifier.setGcmRegId(msg);
			}
		}.execute(null, null, null);
    }

    // Send an upstream message.
    public void onClick(final View view) {

        if (view == findViewById(R.id.send)) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    String msg = "";
                    try {
                        Bundle data = new Bundle();
                        data.putString("my_message", "Hello World");
                        data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
                        String id = Integer.toString(msgId.incrementAndGet());
                        gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                        msg = "Sent message";
                    } catch (IOException ex) {
                        msg = "Error :" + ex.getMessage();
                    }
                    return msg;
                }

                @Override
                protected void onPostExecute(String msg) {
                    mDisplay.append(msg + "\n");
                }
            }.execute(null, null, null);
        } else if (view == findViewById(R.id.clear)) {
            mDisplay.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(DemoActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
      // Your implementation here.
    }
}

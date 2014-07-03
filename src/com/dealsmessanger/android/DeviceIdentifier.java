package com.dealsmessanger.android;

import android.app.Application;

public class DeviceIdentifier extends Application {
	
	private String deviceId;
	
	private String gcmRegId;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getGcmRegId() {
		return gcmRegId;
	}

	public void setGcmRegId(String gcmRegId) {
		this.gcmRegId = gcmRegId;
	}
	
}

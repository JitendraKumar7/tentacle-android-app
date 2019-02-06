package com.sunoray.tentacle.bean;

import java.util.Map;

public class Tracker_updates {
	
	String lng;
	String lat;
	String captured_at;
	Map<String, String> device_info;
	
	public String getLng() {
		return lng;
	}
	public void setLng(String lng) {
		this.lng = lng;
	}
	public String getLat() {
		return lat;
	}
	public void setLat(String lat) {
		this.lat = lat;
	}
	public String getCaptured_at() {
		return captured_at;
	}
	public void setCaptured_at(String captured_at) {
		this.captured_at = captured_at;
	}
	public Map<String, String> getDevice_info() {
		return device_info;
	}
	public void setDevice_info(Map<String, String> device_info) {
		this.device_info = device_info;
	}
}
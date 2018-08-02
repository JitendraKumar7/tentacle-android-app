package com.sunoray.tentacle.bean;

import java.util.List;

public class UserTrackJson {
	
	String unique_session_id;
	List<Tracker_updates> tracker_updates;
	
	public UserTrackJson() {	
	}
	
	public String getUnique_session_id() {
		return unique_session_id;
	}

	public void setUnique_session_id(String unique_session_id) {
		this.unique_session_id = unique_session_id;
	}

	public List<Tracker_updates> getTracker_updates() {
		return tracker_updates;
	}

	public void setTracker_updates(List<Tracker_updates> tracker_updates) {
		this.tracker_updates = tracker_updates;
	}	
}
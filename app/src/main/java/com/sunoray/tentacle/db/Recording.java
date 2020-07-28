package com.sunoray.tentacle.db;

import java.io.Serializable;

public class Recording implements Serializable {

    private static final long serialVersionUID = 6518209424793384925L;
    private long id;
    private String callId;
    private String path;
    private String phoneNumber;
    private String hideNumber;
    private String createdAt;
    private String updatedAt;
    private int numberOfTries;
    private String status;
    private int duration;
    private long startTime;
    private long stopTime;
    private String direction = "Outbound";
    private String audioSrc;
    private String pin;
    private String serverType = "production";
    private int dataSent = 0;
    private String accountId;
    private String campaignId;
    private String prospectId;
    private long dialTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getPath() {
        if (path == null)
            return "";
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getNumberOfTries() {
        return numberOfTries;
    }

    public void setNumberOfTries(int numberOfTries) {
        this.numberOfTries = numberOfTries;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getAudioSrc() {
        return audioSrc;
    }

    public void setAudioSrc(String audioSrc) {
        this.audioSrc = audioSrc;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public int getDataSent() {
        return dataSent;
    }

    public void setDataSent(int dataSent) {
        this.dataSent = dataSent;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getProspectId() {
        return prospectId;
    }

    public void setProspectId(String prospectId) {
        this.prospectId = prospectId;
    }

    public String getHideNumber() {
        return hideNumber;
    }

    public void setHideNumber(String hideNumber) {
        this.hideNumber = hideNumber;
    }

    public long getDialTime() {
        return dialTime;
    }

    public void setDialTime(long dialTime) {
        this.dialTime = dialTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    public long getStartTime() {
        return startTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }
    public long getStopTime() {
        return stopTime;
    }
}
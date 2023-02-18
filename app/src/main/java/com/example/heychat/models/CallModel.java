package com.example.heychat.models;

import java.util.Date;

public class CallModel {
    public String type;
    public User user;
    public Date dataObject;
    public String duration, datetime, cause;
    public Boolean incoming = true;
}

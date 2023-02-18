package com.example.heychat.models;

import java.io.Serializable;

public class User implements Serializable {
    public String name, image, email, token, id;
    public Boolean isSelected = false;
}

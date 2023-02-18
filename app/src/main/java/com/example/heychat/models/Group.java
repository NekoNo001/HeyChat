package com.example.heychat.models;

import java.io.Serializable;
import java.util.List;

public class Group implements Serializable {
    public String name, image, id, token;
    public List<String> member;
}

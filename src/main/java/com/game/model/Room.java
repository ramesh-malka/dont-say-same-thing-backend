package com.game.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Room {

    public String code;
    public String prompt = "Name a fruit";

    public Map<String, Player> players = new ConcurrentHashMap<>();

    public Room(String code) {
        this.code = code;
    }
}

package com.game.controller;

import com.game.model.Player;
import com.game.model.Room;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
public class GameController {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messaging;

    public GameController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    // CREATE ROOM
    @MessageMapping("/create")
    public void createRoom() {
        String code = UUID.randomUUID().toString()
                .substring(0, 4)
                .toUpperCase();

        Room room = new Room(code);
        rooms.put(code, room);

        messaging.convertAndSend("/topic/room/" + code, room);
    }

    // JOIN ROOM
    @MessageMapping("/join")
    public void joinRoom(Map<String, String> payload) {
        String code = payload.get("code");
        String name = payload.get("name");

        Room room = rooms.get(code);
        if (room == null) return;

        room.players.put(name, new Player(name));
        messaging.convertAndSend("/topic/room/" + code, room);
    }

    // SUBMIT ANSWER
    @MessageMapping("/answer")
    public void submitAnswer(Map<String, String> payload) {
        String code = payload.get("code");
        String name = payload.get("name");
        String answer = payload.get("answer");

        Room room = rooms.get(code);
        if (room == null) return;

        Player player = room.players.get(name);
        if (player != null && player.alive) {
            player.answer = answer;
        }
    }

    // END ROUND + ELIMINATION
    @MessageMapping("/end")
    public void endRound(String code) {
        Room room = rooms.get(code);
        if (room == null) return;

        Map<String, List<Player>> grouped =
                room.players.values().stream()
                        .filter(p -> p.alive)
                        .collect(Collectors.groupingBy(
                                p -> normalize(p.answer)
                        ));

        grouped.forEach((answer, list) -> {
            if (list.size() > 1 && !answer.isBlank()) {
                list.forEach(p -> p.alive = false);
            }
        });

        messaging.convertAndSend("/topic/room/" + code, room);
    }

    // CASE-INSENSITIVE NORMALIZATION
    private String normalize(String input) {
        return input == null
                ? ""
                : input.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "");
    }
}

package com.game.controller;

import com.game.model.Player;
import com.game.model.Room;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Game API", description = "Endpoints for multiplayer game")
public class GameController {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    @Operation(summary = "Create a new game room")
    @PostMapping("/rooms")
    public Room createRoom() {
        String code = UUID.randomUUID().toString().substring(0,4).toUpperCase();
        Room room = new Room(code);
        rooms.put(code, room);
        return room;
    }

    @Operation(summary = "Join a game room")
    @PostMapping("/rooms/{code}/join")
    public Room joinRoom(@PathVariable String code, @RequestParam String name) {
        Room room = rooms.get(code);
        if (room == null) throw new RuntimeException("Room not found");
        room.players.put(name, new Player(name));
        return room;
    }

    @Operation(summary = "Submit an answer")
    @PostMapping("/rooms/{code}/answer")
    public Room submitAnswer(@PathVariable String code,
                             @RequestParam String name,
                             @RequestParam String answer) {
        Room room = rooms.get(code);
        if (room == null) throw new RuntimeException("Room not found");
        Player player = room.players.get(name);
        if (player != null && player.alive) player.answer = answer;
        return room;
    }

    @Operation(summary = "End the round (eliminate duplicate answers)")
    @PostMapping("/rooms/{code}/end")
    public Room endRound(@PathVariable String code) {
        Room room = rooms.get(code);
        if (room == null) throw new RuntimeException("Room not found");

        Map<String, List<Player>> grouped =
                room.players.values().stream()
                        .filter(p -> p.alive)
                        .collect(Collectors.groupingBy(p -> normalize(p.answer)));

        grouped.forEach((answer, list) -> {
            if (list.size() > 1 && !answer.isBlank()) {
                list.forEach(p -> p.alive = false);
            }
        });

        return room;
    }

    @Operation(summary = "Get current state of the room")
    @GetMapping("/rooms/{code}")
    public Room getRoom(@PathVariable String code) {
        Room room = rooms.get(code);
        if (room == null) throw new RuntimeException("Room not found");
        return room;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("[^a-z0-9 ]", "");
    }
}

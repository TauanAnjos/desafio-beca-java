package br.com.tauan.ms1_usuarios.domain.model;

import java.time.LocalDateTime;

public class User {

    private final Long id;
    private String name;
    private String email;
    private final LocalDateTime createdAt;

    public User(Long id, String name, String email, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
    }

    public static User newUser(String name, String email) {
        return new User(null, name, email, LocalDateTime.now());
    }

    public void changeData(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
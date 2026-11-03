package com.smartheritage.content.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "languages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Language {
    @Id
    @Column(length = 10)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private boolean active;
}

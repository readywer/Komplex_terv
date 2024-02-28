package org.movie.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
public class Film {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 4096)
    private String description;
    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private List<Category> categories = new ArrayList<>();
    private List<String> actors = new ArrayList<>();
    private int recommendedAge;
}

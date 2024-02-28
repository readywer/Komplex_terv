package org.movie.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String username;
    private String password;
    private String email;
    @Enumerated(EnumType.STRING)
    private Role roles;
}

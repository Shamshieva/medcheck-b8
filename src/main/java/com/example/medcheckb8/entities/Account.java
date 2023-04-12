package com.example.medcheckb8.entities;

import com.example.medcheckb8.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "accounts")
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    @SequenceGenerator(name = "account_seq",allocationSize = 1)
    private Long id;
    private String email;
    private String password;
    private Role role;

    @OneToOne(mappedBy = "account",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private User user;

}
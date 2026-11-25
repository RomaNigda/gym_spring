package io.github.romanigda.gym_spring.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "trainer")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Trainer extends User {
    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "specialization", nullable = false)
    private TrainingType specialization;

    @ManyToMany(mappedBy = "trainers")
    private Set<Trainee> trainees = new HashSet<>();

    @OneToMany(mappedBy = "trainer")
    private List<Training> trainings = new ArrayList<>();

    public Trainer(String firstName, String lastName, boolean active, TrainingType specialization) {
        super(firstName, lastName, active);
        this.specialization = specialization;
    }

    @Override
    public String toString() {
        return "Trainer{" + super.toString()
                + ", specialization=" + specialization + '}';
    }
}

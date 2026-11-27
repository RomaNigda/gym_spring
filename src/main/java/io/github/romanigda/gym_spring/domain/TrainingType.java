package io.github.romanigda.gym_spring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.Objects;

/** Constant dictionary populated by Flyway; never modified by the application. */
@Entity
@Immutable
@Table(name = "training_type")
@Getter
@NoArgsConstructor
public class TrainingType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "training_type_name", nullable = false, unique = true, length = 50)
    private String trainingTypeName;

    public TrainingType(Long id, String trainingTypeName) {
        this.id = id;
        this.trainingTypeName = trainingTypeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrainingType other)) {
            return false;
        }
        return getTrainingTypeName() != null && Objects.equals(getTrainingTypeName(), other.getTrainingTypeName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTrainingTypeName());
    }

    @Override
    public String toString() {
        return "TrainingType{id=" + id + ", trainingTypeName='" + trainingTypeName + "'}";
    }
}

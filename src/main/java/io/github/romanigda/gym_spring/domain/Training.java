package io.github.romanigda.gym_spring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "training")
@Getter
@Setter
@NoArgsConstructor
public class Training {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    @NotBlank
    @Column(name = "training_name", nullable = false)
    private String trainingName;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "training_type_id", nullable = false)
    private TrainingType trainingType;

    @NotNull
    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @NotNull
    @Positive
    @Column(name = "training_duration", nullable = false)
    private Integer trainingDuration;

    public Training(Trainee trainee, Trainer trainer, String trainingName, TrainingType trainingType,
                    LocalDate trainingDate, Integer trainingDuration) {
        this.trainee = trainee;
        this.trainer = trainer;
        this.trainingName = trainingName;
        this.trainingType = trainingType;
        this.trainingDate = trainingDate;
        this.trainingDuration = trainingDuration;
    }

    @Override
    public String toString() {
        return "Training{id=" + id
                + ", trainee=" + (trainee == null ? null : trainee.getUser().getUsername())
                + ", trainer=" + (trainer == null ? null : trainer.getUser().getUsername())
                + ", trainingName='" + trainingName + '\''
                + ", trainingType=" + (trainingType == null ? null : trainingType.getTrainingTypeName())
                + ", trainingDate=" + trainingDate
                + ", trainingDuration=" + trainingDuration + '}';
    }
}

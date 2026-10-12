/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Collections;
import java.util.List;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.ExamDTO;
import org.jspecify.annotations.Nullable;

public class Exam extends ArtemisConnectionHolder {
    private final ExamDTO examDto;
    private final LazyNetworkValue<List<ExamExerciseGroup>> exerciseGroups;

    private final Course course;

    public Exam(ExamDTO examDto, Course course) {
        super(course);
        this.examDto = examDto;
        this.course = course;
        this.exerciseGroups = new LazyNetworkValue<>(() -> {
            var groups = this.examDto.exerciseGroups();
            if (groups == null) {
                return List.of();
            }
            return groups.stream().map(dto -> new ExamExerciseGroup(dto, this)).toList();
        });
    }

    public Course getCourse() {
        return course;
    }

    public long getId() {
        return this.examDto.id();
    }

    public @Nullable String getTitle() {
        return this.examDto.title();
    }

    public List<ExamExerciseGroup> getExerciseGroups() throws ArtemisNetworkException {
        return Collections.unmodifiableList(this.exerciseGroups.get());
    }

    public ExamExerciseGroup getExerciseGroupById(long id) throws ArtemisNetworkException {
        return this.exerciseGroups.get().stream()
                .filter(group -> group.getId() == id)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public String toString() {
        return this.getTitle();
    }
}

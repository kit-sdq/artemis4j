/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Collections;
import java.util.List;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.ExamDTO;

public class Exam extends ArtemisConnectionHolder {
    private final ExamDTO exam;
    private final LazyNetworkValue<List<ExamExerciseGroup>> exerciseGroups;

    private final Course course;

    public Exam(ExamDTO exam, Course course) {
        super(course);
        this.exam = exam;
        this.course = course;
        this.exerciseGroups = new LazyNetworkValue<>(() -> {
            var fullExam = ExamDTO.fetch(this.getConnection().getClient(), this.course.getId(), this.exam.id());
            return fullExam.exerciseGroups().stream()
                    .map(dto -> new ExamExerciseGroup(dto, this))
                    .toList();
        });
    }

    public Course getCourse() {
        return course;
    }

    public long getId() {
        return this.exam.id();
    }

    public String getTitle() {
        return this.exam.title();
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

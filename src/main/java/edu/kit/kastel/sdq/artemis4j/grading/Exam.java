package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.ExamDTO;

import java.util.Collections;
import java.util.List;

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
            return fullExam.exerciseGroups().stream().map(dto -> new ExamExerciseGroup(dto, this)).toList();
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
}

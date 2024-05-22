package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.new_api.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.new_client.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.ExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.SubmissionDTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class Exercise extends ArtemisClientHolder {
    private final ExerciseDTO dto;

    private final Course course;

    public Exercise(ExerciseDTO dto, Course course) {
        super(course);

        this.dto = dto;
        this.course = course;
    }

    public int getId() {
        return this.dto.id();
    }

    public String getTestRespositoryUrl() {
        return this.dto.testRepositoryUri();
    }

    public double getMaxPoints() {
        return this.dto.maxPoints();
    }

    public ZonedDateTime getStartDate() {
        return this.dto.startDate();
    }

    public ZonedDateTime getDueDate() {
        return this.dto.dueDate();
    }

    public String getTitle() {
        return this.dto.title();
    }

    public Course getCourse() {
        return this.course;
    }

    public List<Submission> fetchSubmissions(int correctionRound, boolean onlyOwn) throws ArtemisNetworkException {
        return SubmissionDTO.fetchAll(this.getClient(), this.getId(), correctionRound, onlyOwn)
                .stream()
                .map(dto -> new Submission(dto, this))
                .toList();
    }

    public Submission fetchSubmissionById(long submissionId) throws ArtemisNetworkException {
        return new Submission(SubmissionDTO.fetch(this.getClient(), submissionId), this);
    }

    public Optional<Assessment> tryLockNextSubmission(int correctionRound, GradingConfig gradingConfig) throws AnnotationMappingException, ArtemisNetworkException {
        // This line already locks the submission, but doesn't tell us what the relevant ResultDTO is
        Optional<SubmissionDTO> dto = ExerciseDTO.lockNextSubmission(this.getClient(), this.getId(), correctionRound);
        if (dto.isEmpty()) {
            return Optional.empty();
        }

        Submission submission = new Submission(dto.get(), this);

        // Second lock call to get the ResultDTO
        return submission.tryLock(correctionRound, gradingConfig);
    }
}

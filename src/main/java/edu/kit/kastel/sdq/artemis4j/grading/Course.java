/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.CourseRole;
import edu.kit.kastel.sdq.artemis4j.client.ExamDTO;
import edu.kit.kastel.sdq.artemis4j.client.ExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserPublicInfoDTO;
import org.jspecify.annotations.Nullable;

/**
 * A course, containing exercises.
 */
public class Course extends ArtemisConnectionHolder {
    private final CourseDTO dto;
    private final LazyNetworkValue<List<Exercise>> exercises;
    private final LazyNetworkValue<List<Exam>> exams;

    public Course(CourseDTO dto, ArtemisConnection connection) {
        super(connection);

        this.dto = dto;
        this.exercises = new LazyNetworkValue<>(() -> {
            // Students do not have permission to use the other endpoint, so they are handled here:
            if (this.isStudent(this.getConnection().getAssessor())) {
                List<Exercise> exercises = new ArrayList<>();

                for (ExerciseDTO exerciseDTO :
                        this.dto.exercises() == null ? new ArrayList<ExerciseDTO>() : this.dto.exercises()) {
                    exercises.add(
                            switch (exerciseDTO) {
                                case ProgrammingExerciseDTO programmingExerciseDTO ->
                                    new ProgrammingExercise(programmingExerciseDTO, this);
                                case TextExerciseDTO textExerciseDTO -> new TextExercise(textExerciseDTO, this);
                            });
                }

                return exercises;
            }

            List<Exercise> result =
                    new ArrayList<>(ProgrammingExerciseDTO.fetchAll(connection.getClient(), dto.id()).stream()
                            .map(exerciseDTO -> new ProgrammingExercise(exerciseDTO, this))
                            .toList());

            result.addAll(TextExerciseDTO.fetchAll(connection.getClient(), dto.id()).stream()
                    .map(exerciseDTO -> new TextExercise(exerciseDTO, this))
                    .toList());

            return result;
        });
        this.exams = new LazyNetworkValue<>(() -> ExamDTO.fetchAll(connection.getClient(), dto.id()).stream()
                .map(examDTO -> new Exam(examDTO, this))
                .toList());
    }

    /**
     * Checks if the given user is an instructor of this course.
     *
     * @param user the user to check
     * @return true if the user is an instructor, false otherwise
     */
    public boolean isInstructor(@Nullable User user) throws ArtemisNetworkException {
        return user != null && this.getRoles(user.getLogin()).contains(CourseRole.INSTRUCTOR);
    }

    public boolean isStudent(@Nullable User user) throws ArtemisNetworkException {
        return user != null && this.getRoles(user.getLogin()).contains(CourseRole.STUDENT);
    }

    public Set<CourseRole> getRoles(String userLogin) throws ArtemisNetworkException {
        return this.findUserByLogin(userLogin)
                .map(user -> user.toDTO().getRoles(this.dto))
                .orElse(Set.of());
    }

    public Optional<User> findUserByLogin(String login) throws ArtemisNetworkException {
        var currentUser = this.getConnection().getAssessor();
        if (login.equals(currentUser.getLogin())) {
            return Optional.of(currentUser);
        }

        for (var publicUserInfo : this.searchUser(login, EnumSet.allOf(CourseRole.class))) {
            if (publicUserInfo.login().equals(login)) {
                return Optional.of(new User(publicUserInfo.toUserDTO(this.dto)));
            }
        }

        return Optional.empty();
    }

    /**
     * This endpoint will search for users with the given login or name in this course and filters them by the given roles.
     * <p>
     * Technically this endpoint can return pages of results, but it is a bit broken, so it always returns the first page.
     * Effectively the results are limited to the first 25 users matching the search criteria.
     *
     * @param loginOrName the login or name of the user, note that searching by user id is not possible
     * @param roles the roles to filter by, must not be empty. If you want to search in all roles, use {@link CourseRole#values}.
     * @return a list of users matching the search criteria
     *
     * @throws ArtemisNetworkException if something went wrong while making the request
     */
    public List<UserPublicInfoDTO> searchUser(String loginOrName, Collection<CourseRole> roles)
            throws ArtemisNetworkException {
        return CourseDTO.searchUsers(
                this.getConnection().getClient(), this.getId(), loginOrName, this.dto.mapToGroupNames(roles));
    }

    public long getId() {
        return this.dto.id();
    }

    public String getTitle() {
        return this.dto.title();
    }

    public String getShortName() {
        return this.dto.shortName();
    }

    /**
     * Gets all programming exercises of this course. The result is fetched lazily
     * and then cached.
     */
    public List<ProgrammingExercise> getProgrammingExercises() throws ArtemisNetworkException {
        return this.exercises.get().stream()
                .filter(ProgrammingExercise.class::isInstance)
                .map(ProgrammingExercise.class::cast)
                .toList();
    }

    /**
     * Gets all text exercises of this course. The result is fetched lazily
     * and then cached.
     */
    public List<TextExercise> getTextExercises() throws ArtemisNetworkException {
        return this.exercises.get().stream()
                .filter(TextExercise.class::isInstance)
                .map(TextExercise.class::cast)
                .toList();
    }

    /**
     * Gets a programming exercise by its id.
     *
     * @param id the id of the exercise
     * @return the exercise
     */
    public ProgrammingExercise getProgrammingExerciseById(long id) throws ArtemisNetworkException {
        return this.getProgrammingExercises().stream()
                .filter(e -> e.getId() == id)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No programming exercise with id " + id + " found"));
    }

    /**
     * Gets all exams of this course. The result is fetched lazily and then cached.
     */
    public List<Exam> getExams() throws ArtemisNetworkException {
        return this.exams.get();
    }

    public Exam getExamById(long id) throws ArtemisNetworkException {
        return this.exams.get().stream()
                .filter(e -> e.getId() == id)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No exam with id " + id + " found"));
    }

    public int fetchLockedSubmissionCount() throws ArtemisNetworkException {
        return CourseDTO.fetchLockedSubmissions(this.getConnection().getClient(), this.getId())
                .size();
    }

    public void enrollSelf() throws ArtemisNetworkException {
        long courseId = this.getId();
        var client = this.getConnection().getClient();
        CourseDTO.enrollInCourse(client, courseId);
    }

    public void assignUser(String userLogin, CourseRole role) throws ArtemisNetworkException {
        long courseId = this.getId();
        var client = this.getConnection().getClient();
        CourseDTO.assignUserToCourse(client, courseId, userLogin, role);
    }

    public List<User> fetchAllStudents() throws ArtemisNetworkException {
        return CourseDTO.fetchAllStudents(this.getConnection().getClient(), this.getId()).stream()
                .map(User::new)
                .toList();
    }

    public List<User> fetchAllTutors() throws ArtemisNetworkException {
        return CourseDTO.fetchAllTutors(this.getConnection().getClient(), this.getId()).stream()
                .map(User::new)
                .toList();
    }

    public ProgrammingExercise createProgrammingExercise(ProgrammingExerciseCreateDTO exerciseCreateDTO)
            throws ArtemisNetworkException {
        return this.createProgrammingExercise(exerciseCreateDTO, false);
    }

    public ProgrammingExercise createProgrammingExercise(
            ProgrammingExerciseCreateDTO exerciseCreateDTO, boolean emptyRepositories) throws ArtemisNetworkException {
        var created = ProgrammingExerciseDTO.create(
                this.getConnection().getClient(), exerciseCreateDTO.forCourse(this.getId()), emptyRepositories);
        this.exercises.invalidate();
        return new ProgrammingExercise(created, this);
    }

    public void deleteProgrammingExercise(long exerciseId) throws ArtemisNetworkException {
        this.deleteProgrammingExercise(exerciseId, true);
    }

    public void deleteProgrammingExercise(long exerciseId, boolean deleteBaseReposBuildPlans)
            throws ArtemisNetworkException {
        ProgrammingExerciseDTO.delete(this.getConnection().getClient(), exerciseId, deleteBaseReposBuildPlans);
        this.exercises.invalidate();
    }

    public int getNumberOfStudents() {
        return Objects.requireNonNull(this.dto.numberOfStudents(), "Number of students in the course is null");
    }

    @Override
    public String toString() {
        return this.getTitle();
    }
}

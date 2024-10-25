/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationDTO;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.MistakeType;

/**
 * A single annotation as part of an assessment. Annotations may be manually
 * created, or generated by tools. Annotations may not be created for tests that
 * are executed by Artemis. Annotations must always be created via an active
 * assessment's methods.
 */
public final class Annotation {
    private final String uuid;
    private final MistakeType type;
    private final String filePath;
    private final int startLine;
    private final int endLine;
    private final AnnotationSource source;
    private String customMessage;
    private Double customScore;
    // If not empty, this list contains classifiers that are used to group annotations.
    // For example, all annotations that are related, could have the classifier ["a"],
    // then they would be grouped together.
    //
    // You can add further classifiers to group annotations in a more fine-grained way:
    // For example, when you have annotations with the classifiers ["a", "b"]
    // and ["a", "c"], then if there are more than "annotationLimit"
    // with the classifier "a", it would merge all annotations with the classifiers ["a", "b"]
    // and all annotations with the classifiers ["a", "c"].
    private final List<String> classifiers;
    private final Integer annotationLimit;

    /**
     * Deserializes an annotation from its metajson format
     */
    public Annotation(AnnotationDTO dto, MistakeType mistakeType) {
        this.uuid = dto.uuid();
        this.type = mistakeType;
        this.filePath = dto.classFilePath().replace("\\", "/");
        this.startLine = dto.startLine();
        this.endLine = dto.endLine();
        this.source = dto.source() != null ? dto.source() : AnnotationSource.UNKNOWN;
        this.customMessage = dto.customMessageForJSON();
        this.customScore = dto.customPenaltyForJSON();
        this.classifiers = dto.classifiers() != null ? dto.classifiers() : List.of();
        this.annotationLimit = dto.annotationLimit();
    }

    Annotation(
            MistakeType mistakeType,
            String filePath,
            int startLine,
            int endLine,
            String customMessage,
            Double customScore,
            AnnotationSource source) {
        this(mistakeType, filePath, startLine, endLine, customMessage, customScore, source, List.of(), null);
    }

    Annotation(
            MistakeType mistakeType,
            String filePath,
            int startLine,
            int endLine,
            String customMessage,
            Double customScore,
            AnnotationSource source,
            List<String> classifiers,
            Integer annotationLimit) {
        // Validate custom penalty and message
        if (mistakeType.isCustomAnnotation()) {
            if (customScore == null) {
                throw new IllegalArgumentException("A custom penalty is required for custom annotation types.");
            }
            if (customMessage == null) {
                throw new IllegalArgumentException("A custom message is required for custom annotation types.");
            }
        } else if (customScore != null) {
            throw new IllegalArgumentException("A custom penalty is not allowed for non-custom annotation types.");
        }

        this.uuid = generateUUID();
        this.type = mistakeType;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.customMessage = customMessage;
        this.customScore = customScore;
        this.source = source;
        this.classifiers = new ArrayList<>(classifiers);
        this.annotationLimit = annotationLimit;
    }

    /**
     * Uniquely identifies this annotation
     */
    public String getUUID() {
        return uuid;
    }

    public MistakeType getMistakeType() {
        return type;
    }

    /**
     * The path of the file this annotation is associated with, including its file
     * ending
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * The path of the file this annotation is associated with, excluding its file
     * ending
     */
    public String getFilePathWithoutType() {
        return this.filePath.replace(".java", "");
    }

    /**
     * The line in the file where this annotation starts (0-based)
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * The line in the file where this annotation starts (1-based, for display to
     * the user e.g. in Artemis)
     */
    public int getDisplayLine() {
        return startLine + 1;
    }

    /**
     * The line in the file where this annotation ends (0-based)
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * The custom message associated with this message, if any. Is never empty for
     * custom annotations.
     */
    public Optional<String> getCustomMessage() {
        return Optional.ofNullable(customMessage);
    }

    public void setCustomMessage(String message) {
        if (this.type.isCustomAnnotation() && message == null) {
            throw new IllegalArgumentException("A custom message is required for custom annotation types.");
        }

        this.customMessage = message;
    }

    /**
     * Returns the classifiers of this annotation that can be used to group annotations.
     *
     * @return a list of classifiers
     */
    public List<String> getClassifiers() {
        return new ArrayList<>(this.classifiers);
    }

    /**
     * Returns the maximum number of annotations that should be displayed if one or more classifiers match.
     * <p>
     * It is up to the implementation, how the limit is applied in case of multiple classifiers.
     *
     * @return the maximum number of annotations that should be displayed
     */
    public Optional<Integer> getAnnotationLimit() {
        return Optional.ofNullable(this.annotationLimit);
    }

    /**
     * The custom score associated with this message, if any. Is always empty for
     * predefined annotations, and never empty for custom annotations.
     */
    public Optional<Double> getCustomScore() {
        return Optional.ofNullable(customScore);
    }

    public void setCustomScore(Double score) {
        if (!this.type.isCustomAnnotation() && score != null) {
            throw new IllegalArgumentException("A custom score is not allowed for non-custom annotation types.");
        }
        if (this.type.isCustomAnnotation() && score == null) {
            throw new IllegalArgumentException("A custom score is required for custom annotation types.");
        }

        this.customScore = score;
    }

    public AnnotationSource getSource() {
        return source;
    }

    /**
     * Serializes this annotation to its metajson format
     */
    public AnnotationDTO toDTO() {
        return new AnnotationDTO(
                uuid,
                type.getId(),
                startLine,
                endLine,
                filePath,
                customMessage,
                customScore,
                source,
                classifiers,
                annotationLimit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Annotation that = (Annotation) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}

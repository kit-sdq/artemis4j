package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.metajson.AnnotationDTO;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.MistakeType;

import java.util.Optional;
import java.util.UUID;

public final class Annotation {
    private final String uuid;
    private final MistakeType type;
    private final String filePath;
    private final int startLine;
    private final int endLine;
    private final String customMessage;
    private final Double customPenalty;
    private final AnnotationSource source;

    public Annotation(AnnotationDTO dto, MistakeType mistakeType) {
        this.uuid = dto.uuid();
        this.type = mistakeType;
        this.filePath = dto.classFilePath();
        this.startLine = dto.startLine();
        this.endLine = dto.endLine();
        this.customMessage = dto.customMessageForJSON();
        this.customPenalty = dto.customPenaltyForJSON();
        this.source = AnnotationSource.MANUAL_FIRST_ROUND; // TODO
    }

    public Annotation(MistakeType mistakeType, String filePath, int startLine, int endLine, String customMessage, Double customPenalty, AnnotationSource source) {
        if (!filePath.endsWith(".java")) {
            throw new IllegalArgumentException("File path must end with .java");
        }

        this.uuid = generateUUID();
        this.type = mistakeType;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.customMessage = customMessage;
        this.customPenalty = customPenalty;
        this.source = source;
    }

    public String getUuid() {
        return uuid;
    }

    public MistakeType getMistakeType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFilePathWithoutType() {
        return this.filePath.replace(".java", "");
    }

    public int getStartLine() {
        return startLine;
    }

    public int getDisplayLine() {
        return startLine + 1;
    }

    public int getEndLine() {
        return endLine;
    }

    public Optional<String> getCustomMessage() {
        return Optional.ofNullable(customMessage);
    }

    public Optional<Double> getCustomPenalty() {
        return Optional.ofNullable(customPenalty);
    }

    public AnnotationSource getSource() {
        return source;
    }

    public AnnotationDTO toDTO() {
        return new AnnotationDTO(uuid, type.getId(), startLine, endLine, filePath, customMessage, customPenalty);
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public enum AnnotationSource {
        AUTOGRADER,
        MANUAL_FIRST_ROUND,
        MANUAL_SECOND_ROUND
    }
}

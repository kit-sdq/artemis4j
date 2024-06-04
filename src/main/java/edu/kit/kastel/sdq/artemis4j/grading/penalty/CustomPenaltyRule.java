package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import edu.kit.kastel.sdq.artemis4j.grading.Annotation;

import java.util.List;

public final class CustomPenaltyRule implements PenaltyRule {
    @Override
    public Points calculatePoints(List<Annotation> annotations) {
        return new Points(annotations.stream().mapToDouble(annotation -> annotation.getCustomScore().orElseThrow()).sum(), false);
    }
}

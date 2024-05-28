package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import edu.kit.kastel.sdq.artemis4j.new_api.Annotation;

import java.util.List;

public final class CustomPenaltyRule implements PenaltyRule {
    @Override
    public Points calculatePoints(List<Annotation> annotations) {
        return new Points(annotations.stream().mapToDouble(annotation -> annotation.getCustomPenalty().orElseThrow()).sum(), false);
    }

    @Override
    public boolean isCustomPenalty() {
        return true;
    }
}

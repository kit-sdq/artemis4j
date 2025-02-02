/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.utils.LocationFormatter;

/**
 * Merges annotations based on their classifiers.
 */
final class AnnotationMerger {
    private static final FormatString MERGED_ANNOTATIONS_FORMAT = new FormatString(
            new MessageFormat("{0}Weitere Probleme in {1}.", Locale.GERMAN),
            Map.of(Locale.ENGLISH, new MessageFormat("{0}Other problems in {1}.", Locale.ENGLISH)));

    private AnnotationMerger() {}

    /**
     * Merges annotations based on their classifiers or not if they have none.
     * <p>
     * This method assumes that the annotation uuids are unique.
     *
     * @param unreducedAnnotations the list of annotations to merge
     * @param upperAnnotationLimit the maximum number of annotations for the first classifier
     * @param locale the locale to use for the format string
     * @return the merged list of annotations
     */
    static List<Annotation> mergeAnnotations(
            Collection<Annotation> unreducedAnnotations, int upperAnnotationLimit, Locale locale) {
        // -1 means no limit (useful for unit tests, where one wants to see all annotations)
        if (upperAnnotationLimit == -1) {
            return new ArrayList<>(unreducedAnnotations);
        }

        // first group all problems by the first classifier:
        Map<String, List<Annotation>> groupedAnnotations = unreducedAnnotations.stream()
                .collect(Collectors.groupingBy(
                        annotation ->
                                annotation.getClassifiers().stream().findFirst().orElse(annotation.getUUID()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Annotation> result = new ArrayList<>();
        for (List<Annotation> annotationsForClassifier : groupedAnnotations.values()) {
            // if the annotation limit is set, use it (if it does not exceed the upper limit),
            // otherwise use the upper limit
            int targetNumberOfAnnotations = Math.min(
                    upperAnnotationLimit,
                    annotationsForClassifier.get(0).getAnnotationLimit().orElse(upperAnnotationLimit));

            if (annotationsForClassifier.size() <= targetNumberOfAnnotations) {
                result.addAll(annotationsForClassifier);
                continue;
            }

            // Further partition the annotations by their remaining classifiers:
            Map<String, List<Annotation>> map = annotationsForClassifier.stream()
                    .collect(Collectors.groupingBy(
                            annotation -> {
                                List<String> classifiers = annotation.getClassifiers();
                                if (classifiers.size() <= 1) {
                                    return annotation.getUUID();
                                } else {
                                    // to simplify the grouping code, we merge the remaining classifiers
                                    // into a single string
                                    return String.join(" ", classifiers.subList(1, classifiers.size()));
                                }
                            },
                            LinkedHashMap::new,
                            Collectors.toList()));

            for (List<Annotation> list : map.values()) {
                result.addAll(merge(list, targetNumberOfAnnotations, locale));
            }
        }

        return result;
    }

    private static List<Annotation> merge(List<Annotation> annotations, int limit, Locale locale) {
        // use a dumb algorithm: keep the first limit - 1 annotations, and merge the remainder into a single annotation
        if (annotations.size() <= limit) {
            return annotations;
        }

        List<Annotation> result = new ArrayList<>(annotations.subList(0, limit - 1));
        List<Annotation> toMerge = annotations.subList(limit - 1, annotations.size());

        // first we try to find an annotation with a custom message, this is the main one that will be shown to the user
        int firstIndexWithCustomMessage = 0;
        for (int i = 0; i < toMerge.size(); i++) {
            if (toMerge.get(i).getCustomMessage().isPresent()) {
                firstIndexWithCustomMessage = i;
                break;
            }
        }

        // the first annotation with the custom message is removed, so it doesn't appear twice
        Annotation firstAnnotation = toMerge.remove(firstIndexWithCustomMessage);
        // if there are no annotations with a custom message, the firstIndexWithCustomMessage will be 0,
        // and because it doesn't have a custom message it will be null
        String message = firstAnnotation.getCustomMessage().orElse(null);
        if (message == null) {
            message = "";
        } else {
            // some messages might not end with a period, which would look weird with the above format string,
            // so this adds one if necessary
            if (!message.endsWith(".")) {
                message += ".";
            }

            message += " ";
        }

        String customMessage = MERGED_ANNOTATIONS_FORMAT
                .format(message, displayLocations(firstAnnotation, toMerge))
                .translateTo(locale);

        result.add(new Annotation(
                firstAnnotation.getMistakeType(),
                firstAnnotation.getLocation(),
                customMessage,
                firstAnnotation.getCustomScore().orElse(null),
                firstAnnotation.getSource()));

        return result;
    }

    private static String displayLocations(Annotation first, Iterable<Annotation> others) {
        LocationFormatter formatter = new LocationFormatter();
        for (var annotation : others) {
            formatter.addLocation(annotation.getLocation());
        }

        return formatter
                .removeSharedPrefix(prefix -> first.getFilePath().startsWith(prefix))
                .removeExtension(true)
                .format();
    }
}

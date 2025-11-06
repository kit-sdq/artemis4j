/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;
import org.jspecify.annotations.Nullable;

public final class RatingGroup {
    // Subgroups are indicated by a double colon "::" in the id.
    // For example "modelling::oop" would indicate a subgroup "oop" in the rating group "modelling".
    private static final String SUBGROUP_SEPARATOR = "::";

    private final String id;
    private final FormatString displayName;
    private final double minPenalty;
    private final double maxPenalty;
    private final List<MistakeType> mistakeTypes;
    private final Map<String, RatingGroup> subRatingGroups;
    private final @Nullable RatingGroup parentRatingGroup;

    private static double parseLimit(
            double limit,
            @Nullable Double parentLimit,
            BiFunction<? super Double, ? super Double, String> errorMessageBuilder) {
        // This ensures that the limit is always positive. When there is a negative limit,
        // negate it to make it positive and then later negate it again.
        //
        // This works, for example given limit -5 and parentLimit -10:
        // 1. limit = -5, parentLimit = -10
        // 2. limit = 5, parentLimit = 10
        // -> result = 5 -> -5
        if (limit < 0.0) {
            return -parseLimit(
                    -limit,
                    parentLimit == null ? null : -parentLimit,
                    (current, parent) -> errorMessageBuilder.apply(-current, -parent));
        }

        double result = limit;
        if (parentLimit != null) {
            // if the limit is positive infinity, there was no limit defined
            // -> inherit the limit from the parent group

            // The limit of this group should not be larger than the limit of the parent group.
            // e.g. parent group has a limit of 5 and this group has a limit of 10, which is invalid.
            if (limit < Double.POSITIVE_INFINITY && parentLimit < result) {
                throw new IllegalArgumentException(errorMessageBuilder.apply(result, parentLimit));
            }

            // If the parent group has a positive limit, the smaller limit is prioritized.
            //
            // For example, if the parent group has a limit of 10 and this group has a limit of 5,
            // the limit should be 5.
            result = Math.min(result, parentLimit);
        }

        return result;
    }

    private RatingGroup(RatingGroupDTO dto, @Nullable RatingGroup parentRatingGroup) {
        // The negative limit defines the amount of points that can be subtracted at most.
        double negativeLimit = parseLimit(
                dto.negativeLimit() == null ? Double.NEGATIVE_INFINITY : dto.negativeLimit(),
                parentRatingGroup == null ? null : parentRatingGroup.getMinPenalty(),
                (currentLimit, parentLimit) ->
                        "The negative limit of the subgroup %s (%fP) is smaller than the limit of its parent group %s (%fP)"
                                .formatted(
                                        dto.shortName(),
                                        currentLimit,
                                        Objects.requireNonNull(parentRatingGroup)
                                                .getId(),
                                        parentLimit));

        // The positive limit defines the amount of points that can be given at most.
        double positiveLimit = parseLimit(
                dto.positiveLimit() == null ? Double.POSITIVE_INFINITY : dto.positiveLimit(),
                parentRatingGroup == null ? null : parentRatingGroup.getMaxPenalty(),
                (currentLimit, parentLimit) ->
                        "The positive limit of the subgroup %s (%fP) is larger than the limit of its parent group %s (%fP)"
                                .formatted(
                                        dto.shortName(),
                                        currentLimit,
                                        Objects.requireNonNull(parentRatingGroup)
                                                .getId(),
                                        parentLimit));

        // sanity check that everything was calculated correctly
        if (negativeLimit > positiveLimit) {
            throw new IllegalArgumentException("Invalid penalty range for rating group: %fP -- %fP (%fP -- %fP)"
                    .formatted(dto.negativeLimit(), dto.positiveLimit(), negativeLimit, positiveLimit));
        }

        this.id = dto.shortName();
        this.displayName = new FormatString(dto.displayName(), dto.additionalDisplayNames());
        this.minPenalty = negativeLimit;
        this.maxPenalty = positiveLimit;
        this.mistakeTypes = new ArrayList<>();

        // Each rating group is aware of its parent (if it is a subgroup) and its subgroups (if there are any):
        this.subRatingGroups = new LinkedHashMap<>();
        this.parentRatingGroup = parentRatingGroup;
    }

    static List<RatingGroup> createRatingGroups(Iterable<RatingGroupDTO> dtos) throws InvalidGradingConfigException {
        Map<String, RatingGroup> ratingGroupsById = new LinkedHashMap<>();
        // The subgroups might appear before the parent in the list of groups, that's
        // why they are collected separately first
        Collection<RatingGroupDTO> subRatingGroups = new ArrayList<>();
        for (RatingGroup.RatingGroupDTO ratingGroupDTO : dtos) {
            String id = ratingGroupDTO.shortName();

            if (id.contains(RatingGroup.SUBGROUP_SEPARATOR)) {
                subRatingGroups.add(ratingGroupDTO);
            } else {
                ratingGroupsById.put(id, new RatingGroup(ratingGroupDTO, null));
            }
        }

        for (var ratingGroup : subRatingGroups) {
            String id = ratingGroup.shortName();
            String parentId = id.substring(0, id.indexOf(RatingGroup.SUBGROUP_SEPARATOR));
            RatingGroup parent = ratingGroupsById.get(parentId);
            if (parent == null) {
                throw new InvalidGradingConfigException(
                        "Subgroup '%s' has no parent group with id '%s'".formatted(id, parentId));
            }
            parent.addSubGroup(id, ratingGroup);
        }

        return new ArrayList<>(ratingGroupsById.values());
    }

    /**
     * Adds the given rating group to this group assuming that it is a subgroup of this.
     *
     * @param id the id of the subgroup, starting with the id of this group, like modelling::oop
     * @param subGroupDto the DTO of the subgroup
     * @throws IllegalArgumentException if the id does not start with this group's id
     * @throws IllegalArgumentException if the id is not for a subgroup (nothing separated by "::")
     * @throws IllegalArgumentException if the id is for a nested subgroup (e.g. modelling::oop::subgroup)
     * @throws IllegalArgumentException if the subgroup already exists
     */
    private void addSubGroup(String id, RatingGroupDTO subGroupDto) {
        var path = id.split(SUBGROUP_SEPARATOR, -1);
        if (path.length < 2) {
            throw new IllegalArgumentException("Invalid subgroup path: %s".formatted(id));
        }

        if (!this.id.equals(path[0])) {
            throw new IllegalArgumentException("Subgroup %s does not belong to group %s".formatted(id, this.id));
        }

        if (path[1].contains(SUBGROUP_SEPARATOR)) {
            throw new IllegalArgumentException("Subgroup %s is a nested subgroup".formatted(id));
        }

        if (this.subRatingGroups.containsKey(path[1])) {
            throw new IllegalArgumentException("Subgroup %s already exists".formatted(id));
        }

        this.subRatingGroups.put(path[1], new RatingGroup(subGroupDto, this));
    }

    /**
     * Finds the rating group with the given id in this group and its subgroups.
     *
     * @param id the id of the group to find
     * @return an optional with the group if it was found, empty otherwise
     */
    Optional<RatingGroup> findGroup(String id) {
        if (this.id.equals(id)) {
            return Optional.of(this);
        }

        for (var entry : this.listSubGroups()) {
            var group = entry.findGroup(id);
            if (group.isPresent()) {
                return group;
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the parent of this rating group, if any.
     *
     * @return the parent group or null if this is a top-level group
     */
    public @Nullable RatingGroup getParent() {
        return this.parentRatingGroup;
    }

    /**
     * Returns the list of all subgroups of this group, if any.
     *
     * @return the subgroups or an empty list if there are none
     */
    public List<RatingGroup> listSubGroups() {
        return List.copyOf(this.subRatingGroups.values());
    }

    /**
     * Returns the list of all mistake types in this group and its subgroups.
     *
     * @return a list with all mistake types
     */
    public List<MistakeType> getAllMistakeTypes() {
        List<MistakeType> result = new ArrayList<>(this.mistakeTypes);

        for (var group : this.subRatingGroups.values()) {
            result.addAll(group.getAllMistakeTypes());
        }

        return Collections.unmodifiableList(result);
    }

    public String getId() {
        return id;
    }

    public TranslatableString getDisplayName() {
        return displayName.format();
    }

    public double getMinPenalty() {
        return minPenalty;
    }

    public double getMaxPenalty() {
        return maxPenalty;
    }

    public boolean isScoringGroup() {
        return this.getMinPenalty() != 0 || this.getMaxPenalty() != 0;
    }

    private List<RatingGroup> parents() {
        List<RatingGroup> parents = new ArrayList<>();
        RatingGroup parent = this.parentRatingGroup;
        while (parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }
        return parents;
    }

    boolean isAssociatedWith(RatingGroup ratingGroup) {
        if (this.equals(ratingGroup)) {
            return true;
        }

        for (var parent : this.parents()) {
            if (parent.equals(ratingGroup)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Only used during deserialization to create the circular references between
     * RatingGroup and MistakeType
     */
    void addMistakeType(MistakeType mistakeType) {
        this.mistakeTypes.add(mistakeType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingGroup that = (RatingGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "RatingGroup[" + "id=" + id + ", " + "displayName=" + displayName.getDefaultTranslationPattern() + ", "
                + "minPenalty=" + minPenalty + ", " + "getMaxPenalty=" + maxPenalty + ']';
    }

    record RatingGroupDTO(
            String shortName,
            String displayName,
            @Nullable Double positiveLimit,
            @Nullable Double negativeLimit,
            Map<String, String> additionalDisplayNames) {}
}

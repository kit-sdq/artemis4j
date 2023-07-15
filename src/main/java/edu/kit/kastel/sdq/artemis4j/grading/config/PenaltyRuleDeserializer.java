/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.config;

import edu.kit.kastel.sdq.artemis4j.grading.model.rule.PenaltyRule;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;

/**
 * Deserialize a PenaltyRule which is not trivial since PenaltyRule is abstract
 * and its subclasses have individual constructor signatures. Thus, this must be
 * done manually. Penalty rule types are defined here (see
 * {@link PenaltyRuleType}) and by extending {@link PenaltyRule} in the model
 * package.
 */
public class PenaltyRuleDeserializer extends StdDeserializer<PenaltyRule> {
	@Serial
	private static final long serialVersionUID = 6326274512036616184L;

	protected PenaltyRuleDeserializer() {
		super(PenaltyRule.class);
	}

	@Override
	public PenaltyRule deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
		final JsonNode penaltyRuleNode = parser.getCodec().readTree(parser);
		final String penaltyRuleShortName = penaltyRuleNode.get("shortName").asText();

		final PenaltyRuleType penaltyRuleType = PenaltyRuleType.fromShortName(penaltyRuleShortName);
		if (penaltyRuleType != null) {
			return penaltyRuleType.construct(penaltyRuleNode);
		}
		throw new IOException("No PenaltyRule Subclass defined for penaltyRule " + penaltyRuleShortName);
	}
}

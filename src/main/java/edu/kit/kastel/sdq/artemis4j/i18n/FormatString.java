/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.i18n;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class FormatString {
	public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

	private final Map<Locale, MessageFormat> translations;
	private final MessageFormat defaultTranslation;

	public FormatString(String defaultTranslation) {
		this(new MessageFormat(defaultTranslation));
	}

	public FormatString(MessageFormat defaultTranslation) {
		this.defaultTranslation = defaultTranslation;
		this.translations = Map.of();
	}

	public FormatString(String defaultTranslation, Map<String, String> additionalTranslations) {
		this(escapeStringForMessageFormat(defaultTranslation, DEFAULT_LOCALE),
				additionalTranslations == null ? null
						: additionalTranslations.entrySet().stream().collect(Collectors.toMap(e -> Locale.forLanguageTag(e.getKey()),
								e -> escapeStringForMessageFormat(e.getValue(), Locale.forLanguageTag(e.getKey())))));
	}

	public FormatString(MessageFormat defaultTranslation, Map<Locale, MessageFormat> additionalTranslations) {
		this.defaultTranslation = defaultTranslation;
		if (additionalTranslations == null) {
			this.translations = Map.of();
		} else {
			this.translations = new HashMap<>(additionalTranslations);
		}
	}

	public TranslatableString format(Object... args) {
		return new TranslatableString(this, args);
	}

	String translateTo(Locale locale, Object... args) {
		if (locale == null) {
			return this.defaultTranslation.format(args);
		}

		var formatString = this.translations.getOrDefault(locale, this.defaultTranslation);
		try {
			return formatString.format(args);
		} catch (IllegalArgumentException e) {
			// format(...) throws an IllegalArgumentException for all kinds of failures
			// We just want to add some info to make debugging easier
			throw new FormatException("Failed to format string '%s' with locale '%s' and %d arguments of types (%s)".formatted(formatString.toPattern(), locale,
					args.length, Arrays.stream(args).map(a -> a.getClass().getTypeName()).collect(Collectors.joining(","))), e);
		}
	}

	@Override
	public String toString() {
		throw new IllegalStateException("Format & translate this string before using it.");
	}

	private static MessageFormat escapeStringForMessageFormat(String string, Locale locale) {
		return new MessageFormat("'" + string.replace("'", "''") + "'", locale);
	}

	public static class FormatException extends RuntimeException {
		public FormatException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}

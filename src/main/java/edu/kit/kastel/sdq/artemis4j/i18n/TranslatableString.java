package edu.kit.kastel.sdq.artemis4j.i18n;

import java.text.MessageFormat;
import java.util.Locale;

public class TranslatableString {
    private final FormatString formatString;
    private final Object[] args;

    TranslatableString(FormatString formatString, Object[] args) {
        this.formatString = formatString;
        this.args = args;
    }

    public String translateTo(String language) {
        return this.translateTo(Locale.of(language));
    }

    public String translateTo(Locale locale) {
        // Translate all sub-strings
        var translatedArgs = new Object[this.args.length];
        for (int i = 0; i < this.args.length; i++) {
            if (this.args[i] instanceof TranslatableString translatableString) {
                translatedArgs[i] = translatableString.translateTo(locale);
            } else {
                translatedArgs[i] = this.args[i];
            }
        }
        return this.formatString.translateTo(locale, translatedArgs);
    }

    @Override
    public String toString() {
        throw new IllegalStateException("Translate this string to a specific language before using it.");
    }

    private static MessageFormat escapeStringForMessageFormat(String string) {
        return new MessageFormat("'" + string.replace("'", "''") + "'");
    }
}

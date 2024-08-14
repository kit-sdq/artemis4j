/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.i18n;

import java.util.Locale;

public class TranslatableString {
    private final FormatString formatString;
    private final Object[] args;

    TranslatableString(FormatString formatString, Object[] args) {
        this.formatString = formatString;
        this.args = args;
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

    public String translateToDefault() {
        return this.translateTo(null);
    }

    @Override
    public String toString() {
        return this.translateToDefault();
    }
}

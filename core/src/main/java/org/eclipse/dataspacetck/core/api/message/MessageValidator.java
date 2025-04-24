package org.eclipse.dataspacetck.core.api.message;

import com.fasterxml.jackson.databind.JsonNode;

public interface MessageValidator {
    ValidationResult validate(JsonNode message);

    record ValidationResult(String message) {

        public static ValidationResult valid() {
            return new ValidationResult(null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(message);
        }

        public boolean isValid() {
            return message == null;
        }

        public String getMessage() {
            return message;
        }
    }
}



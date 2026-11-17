package org.jetuml.validation;

/**
 * Resultado da validacao estrutural de uma operacao no diagrama UML (RF18, RF19, RF30).
 */
public final class ValidationResult {
    private final boolean valid;
    private final boolean silent;
    private final String errorMessage;

    private ValidationResult(boolean valid, boolean silent, String errorMessage) {
        this.valid = valid;
        this.silent = silent;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, false, null);
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(false, false, message);
    }

    /**
     * Usado quando uma tentativa invalida no Diagrama de Sequencia deve falhar silenciosamente
     * sem exibir notificacao na interface (RF30).
     */
    public static ValidationResult silentReject() {
        return new ValidationResult(false, true, null);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isSilent() {
        return silent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

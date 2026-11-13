package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;

import java.util.Map;

/** A whitelisted business operation, independent of the language-model provider. */
public interface AssistantTool {
    /** Returns the unique machine-readable name. */
    String getName();

    /** Describes what the tool does, for the assistant and its user. */
    String getDescription();

    /** Returns parameter names and descriptions; descriptions indicate optional values. */
    Map<String, String> getParameters();

    /** Indicates whether dispatch must wait for explicit user confirmation. */
    boolean requiresConfirmation();

    /** Runs through services; callers must use ToolRegistry to enforce confirmation. */
    String execute(Map<String, String> parameters)
        throws ValidationException, BusinessRuleException, DataAccessException;
}

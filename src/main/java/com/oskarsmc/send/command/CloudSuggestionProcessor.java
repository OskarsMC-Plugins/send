package com.oskarsmc.send.command;

import cloud.commandframework.execution.CommandSuggestionProcessor;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import com.velocitypowered.api.command.CommandSource;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class CloudSuggestionProcessor implements CommandSuggestionProcessor<CommandSource> {

    @Override
    public @NonNull List<String> apply(@NonNull CommandPreprocessingContext<CommandSource> context, @NonNull List<String> strings) {
        String currentInput;

        if (context.getInputQueue().isEmpty()) {
            currentInput = "";
        } else {
            currentInput = context.getInputQueue().peek();
        }

        currentInput = currentInput.toLowerCase();
        ArrayList<String> suggestions = new ArrayList<>();

        for (String suggestion : strings) {
            if (suggestion.toLowerCase().startsWith(currentInput)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }
}
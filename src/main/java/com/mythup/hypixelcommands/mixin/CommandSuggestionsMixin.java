package com.mythup.hypixelcommands.mixin;

import com.mythup.hypixelcommands.HypixelCommandsMod;
import com.mythup.hypixelcommands.brigadier.HypixelCommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private EditBox input;

    private boolean hypixelCustomOverrodeSuggestions;

    @Inject(method = "updateCommandInfo", at = @At("TAIL"))
    private void onUpdateCommandInfo(CallbackInfo ci) {
        String inputText = input == null ? null : input.getValue();
        System.out.println("[HypixelCommands] updateCommandInfo input='" + inputText + "' isHypixel=" + HypixelCommandsMod.isHypixel());

        if (!HypixelCommandsMod.isHypixel()) {
            hypixelCustomOverrodeSuggestions = false;
            return;
        }

        if (inputText == null || inputText.isEmpty()) {
            System.out.println("[HypixelCommands] skip (empty command)");
            hypixelCustomOverrodeSuggestions = false;
            return;
        }

        if (!inputText.startsWith("/")) {
            inputText = "/" + inputText;
        }

        Suggestions custom = HypixelCommandDispatcher.getSuggestions(inputText);
        if (custom == null || custom.isEmpty()) {
            System.out.println("[HypixelCommands] no local suggestions for this command input; clearing stale local override");
            if (hypixelCustomOverrodeSuggestions) {
                this.pendingSuggestions = Suggestions.empty();
                try {
                    CommandSuggestions self = (CommandSuggestions) (Object) this;
                    java.lang.reflect.Field suggestionsField = CommandSuggestions.class.getDeclaredField("suggestions");
                    suggestionsField.setAccessible(true);
                    suggestionsField.set(self, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                hypixelCustomOverrodeSuggestions = false;
            }
            return;
        }

        System.out.println("[HypixelCommands] applying custom suggestions range=" + custom.getRange() + " list=" + custom.getList());
        this.pendingSuggestions = CompletableFuture.completedFuture(custom);
        hypixelCustomOverrodeSuggestions = true;
    }
}
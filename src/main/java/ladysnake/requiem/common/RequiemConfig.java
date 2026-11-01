/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ladysnake.requiem.Requiem;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RequiemConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "requiem.json");

    public static final Graphics graphics = new Graphics();
    public static final Speech speech = new Speech();

    public static class Graphics {
        public boolean fancyDemonRender = true;
        /** Disables full-screen post-processing effects (accessibility / low-end GPUs). */
        public boolean reducedEffects = false;
        /** Multiplier for ghost particle spawn rate, from 0 (none) to 1 (default). */
        public float particleDensity = 1.0f;
    }

    public static class Speech {
        /** Automatically translate Requiem speech bubbles into each viewer's Minecraft language. */
        public boolean translateBubbles = true;
        /** "google" uses Google's free endpoint; "libretranslate" uses the URL below. */
        public String translationProvider = "google";
        /** Base URL of a LibreTranslate instance; /translate is appended automatically. */
        public String libreTranslateUrl = "http://127.0.0.1:5000";
        /** Optional key for hosted/self-hosted LibreTranslate instances. */
        public String libreTranslateApiKey = "";
        /** Used until a client reports its Minecraft language. */
        public String defaultLanguage = "en";
        /** HTTP timeout in seconds. */
        public int timeoutSeconds = 8;
        /** Server-wide integration toggle for Say It English challenge system. */
        public boolean sayItGloballyEnabled = true;
    }

    private static class ConfigData {
        public boolean fancyDemonRender = true;
        public boolean reducedEffects = false;
        public float particleDensity = 1.0f;
        public boolean translateBubbles = true;
        public String translationProvider = "google";
        public String libreTranslateUrl = "http://127.0.0.1:5000";
        public String libreTranslateApiKey = "";
        public String defaultLanguage = "en";
        public int timeoutSeconds = 8;
        public boolean sayItGloballyEnabled = true;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    graphics.fancyDemonRender = data.fancyDemonRender;
                    graphics.reducedEffects = data.reducedEffects;
                    graphics.particleDensity = Math.max(0.0f, Math.min(1.0f, data.particleDensity));
                    speech.translateBubbles = data.translateBubbles;
                    speech.translationProvider = "libretranslate".equalsIgnoreCase(data.translationProvider)
                        ? "libretranslate" : "google";
                    speech.libreTranslateUrl = data.libreTranslateUrl == null || data.libreTranslateUrl.isBlank()
                        ? "http://127.0.0.1:5000" : data.libreTranslateUrl.trim();
                    speech.libreTranslateApiKey = data.libreTranslateApiKey == null ? "" : data.libreTranslateApiKey;
                    speech.defaultLanguage = data.defaultLanguage == null || data.defaultLanguage.isBlank()
                        ? "en" : data.defaultLanguage.trim().toLowerCase(java.util.Locale.ROOT);
                    speech.timeoutSeconds = Math.max(2, Math.min(15, data.timeoutSeconds));
                    speech.sayItGloballyEnabled = data.sayItGloballyEnabled;
                    SayItCompatibilityManager.setGlobalEnabled(data.sayItGloballyEnabled);
                }
            } catch (Exception e) {
                Requiem.LOGGER.error("[Requiem] Failed to load config", e);
            }
        }
        save();
    }

    public static void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                ConfigData data = new ConfigData();
                data.fancyDemonRender = graphics.fancyDemonRender;
                data.reducedEffects = graphics.reducedEffects;
                data.particleDensity = graphics.particleDensity;
                data.translateBubbles = speech.translateBubbles;
                data.translationProvider = speech.translationProvider;
                data.libreTranslateUrl = speech.libreTranslateUrl;
                data.libreTranslateApiKey = speech.libreTranslateApiKey;
                data.defaultLanguage = speech.defaultLanguage;
                data.timeoutSeconds = speech.timeoutSeconds;
                data.sayItGloballyEnabled = speech.sayItGloballyEnabled;
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            Requiem.LOGGER.error("[Requiem] Failed to save config", e);
        }
    }
}

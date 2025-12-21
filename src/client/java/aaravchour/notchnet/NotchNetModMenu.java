package aaravchour.notchnet;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class NotchNetModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("NotchNet Configuration"));

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General Settings"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // API URL Field
            general.addEntry(entryBuilder.startStrField(Text.literal("API URL"), NotchNetConfig.apiUrl)
                    .setDefaultValue("http://localhost:8000")
                    .setSaveConsumer(newValue -> NotchNetConfig.apiUrl = newValue)
                    .build());

            // Auto Scan Mods Toggle
            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Scan Mods"), NotchNetConfig.autoScanMods)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> NotchNetConfig.autoScanMods = newValue)
                    .build());

            // Use Advanced Mode Toggle
            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Use Advanced Mode"), NotchNetConfig.useAdvancedMode)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> NotchNetConfig.useAdvancedMode = newValue)
                    .build());

            builder.setSavingRunnable(NotchNetConfig::saveConfig);
            
            return builder.build();
        };
    }
}

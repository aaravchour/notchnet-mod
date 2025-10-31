package aaravchour.notchnet;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class NotchNetModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            // Create Cloth Config screen
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("NotchNet Config"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // General category
            builder.getOrCreateCategory(Text.literal("General"))
                    .addEntry(entryBuilder.startBooleanToggle(
                                            Text.literal("Use Advanced Mode"),
                                            NotchNetConfig.useAdvancedMode
                                    )
                                    .setSaveConsumer(newValue -> NotchNetConfig.useAdvancedMode = newValue)
                                    .setDefaultValue(true) // optional default
                                    .build()
                    );

            return builder.build();
        };
    }
}

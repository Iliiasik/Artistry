package iliiasik.artistry.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import iliiasik.artistry.client.ui.screen.config.ArtistryConfigScreen;

public final class ArtistryModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ArtistryConfigScreen::new;
    }
}

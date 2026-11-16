package media.hofferbert.astralvigil.datagen;

import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** en_us / de_de from the shared {@link AVLang} table. */
public class AVLanguageProvider extends LanguageProvider {
    private final String locale;

    public AVLanguageProvider(PackOutput output, String locale) {
        super(output, AstralVigil.MOD_ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        boolean german = locale.equals("de_de");
        for (AVLang.Entry e : AVLang.entries()) {
            add(e.key(), german ? e.de() : e.en());
        }
    }
}

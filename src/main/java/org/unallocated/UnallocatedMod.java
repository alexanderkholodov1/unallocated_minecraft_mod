package org.unallocated;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnallocatedMod implements ModInitializer {
    public static final String MOD_ID = "unallocated";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        org.unallocated.core.CoreRuntime.register(new org.unallocated.ending.EpilogueAct());
        org.unallocated.core.CoreRuntime.initialize();
        LOGGER.info("UNALLOCATED initialized (common).");
    }
}

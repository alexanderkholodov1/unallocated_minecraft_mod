package org.unallocated.client.mixin.interfacefxsmoke;

import java.nio.file.Files;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CI-only observer: fails if production recovery ran after Options.load instead of before. */
@Mixin(Options.class)
abstract class InterfaceRecoveryProbe {
    @Unique private static boolean interfaceSmoke$checked;

    @Inject(method = "load", at = @At("RETURN"))
    private void interfaceSmoke$assertRecoveredBeforeRead(CallbackInfo ci) {
        if (interfaceSmoke$checked) return;
        var directory = Minecraft.getInstance().gameDirectory.toPath();
        if (!Files.exists(directory.resolve("interface-original-options.bin"))) {
            throw new AssertionError("Missing crash recovery fixture");
        }
        var options = (Options) (Object) this;
        if (options.sensitivity().get() != .713 || options.fov().get() != 93 || options.gamma().get() != .43) {
            throw new AssertionError("Vanilla read corrupted options before journal recovery");
        }
        try {
            if (!Arrays.equals(Files.readAllBytes(directory.resolve("interface-original-options.bin")),
                    Files.readAllBytes(directory.resolve("options.txt")))) {
                throw new AssertionError("Startup recovery changed the original bytes");
            }
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
        interfaceSmoke$checked = true;
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED interface smoke: original options recovered before vanilla load.");
    }
}

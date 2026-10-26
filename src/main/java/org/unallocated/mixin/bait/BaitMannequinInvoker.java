package org.unallocated.mixin.bait;

import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The vanilla mannequin keeps these setters private in 26.3; the spyglass bait needs them to dress the figure. */
@Mixin(Mannequin.class)
public interface BaitMannequinInvoker {
    @Invoker("setProfile")
    void unallocated$setProfile(ResolvableProfile profile);

    @Invoker("setImmovable")
    void unallocated$setImmovable(boolean immovable);

    @Invoker("setHideDescription")
    void unallocated$setHideDescription(boolean hide);
}

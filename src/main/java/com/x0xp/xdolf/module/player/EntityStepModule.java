package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class EntityStepModule extends ClientModule {
    private final NumberSetting height = setting("height", 2, 1, 256, 1);
    private final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Xdolf.ID, "entity_step");
    private LivingEntity owner;

    EntityStepModule() {
        super("EntityStep", "Increase step height for a controlled living mount.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {
        var vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof LivingEntity living) || vehicle.getControllingPassenger() != mc.player) {
            reset(mc);
            return;
        }
        if (owner != living) {
            reset(mc);
            owner = living;
        }
        var attribute = owner.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute == null) return;
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id,
            Math.max(0, height.get() - attribute.getValue()), AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public void reset(Minecraft mc) {
        if (owner != null && owner.getAttribute(Attributes.STEP_HEIGHT) != null)
            owner.getAttribute(Attributes.STEP_HEIGHT).removeModifier(id);
        owner = null;
    }
}

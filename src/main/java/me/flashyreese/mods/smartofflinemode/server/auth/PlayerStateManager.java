package me.flashyreese.mods.smartofflinemode.server.auth;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateManager {
    private final Map<UUID, State> playerStateMap = new HashMap<>();

    public boolean trackState(PlayerEntity entity) {
        if (this.playerStateMap.containsKey(entity.getUuid()))
            return false;

        this.playerStateMap.put(entity.getUuid(), State.of(entity));
        return true;
    }

    public boolean restoreState(PlayerEntity entity) {
        if (this.playerStateMap.containsKey(entity.getUuid())) {
            State state = this.playerStateMap.get(entity.getUuid());
            entity.removeStatusEffect(StatusEffects.BLINDNESS);
            if (state.blindness != null) {
                entity.addStatusEffect(state.blindness);
            }
            entity.removeStatusEffect(StatusEffects.INVISIBILITY);
            if (state.invisible != null) {
                entity.addStatusEffect(state.invisible);
            }
            entity.teleport(state.position.getX(), state.position.getY(), state.position.getZ());
            entity.setInvulnerable(state.vulnerable);
            return true;
        }
        return false;
    }

    public static class State {
        private final boolean vulnerable;
        private final BlockPos position;
        private final StatusEffectInstance blindness;
        private final StatusEffectInstance invisible;

        public State(boolean vulnerable, BlockPos position, StatusEffectInstance blindness, StatusEffectInstance invisible) {
            this.vulnerable = vulnerable;
            this.position = position;
            this.blindness = blindness;
            this.invisible = invisible;
        }

        public static State of(PlayerEntity entity) {
            return new State(entity.isInvulnerable(), entity.getBlockPos(), entity.hasStatusEffect(StatusEffects.BLINDNESS) ? entity.getStatusEffect(StatusEffects.BLINDNESS) : null, entity.hasStatusEffect(StatusEffects.INVISIBILITY) ? entity.getStatusEffect(StatusEffects.INVISIBILITY) : null);
        }
    }
}

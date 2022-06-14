package me.flashyreese.mods.smartofflinemode.server.auth;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerStateManager {
    private final Long2ObjectOpenHashMap<State> playerStateMap = new Long2ObjectOpenHashMap<>();

    public boolean trackState(PlayerEntity entity) {
        if (this.playerStateMap.containsKey(entity.getUuid().getMostSignificantBits()))
            return false;

        this.playerStateMap.put(entity.getUuid().getMostSignificantBits(), State.of(entity));
        return true;
    }

    public void isolateState(PlayerEntity entity) {
        entity.setInvulnerable(true);
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, Integer.MAX_VALUE));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE));
        entity.teleport(0D, 2560D, 0D);
    }

    public boolean restoreState(PlayerEntity entity) {
        if (this.playerStateMap.containsKey(entity.getUuid().getMostSignificantBits())) {
            State state = this.playerStateMap.get(entity.getUuid().getMostSignificantBits());
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
            this.playerStateMap.remove(entity.getUuid().getMostSignificantBits());
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

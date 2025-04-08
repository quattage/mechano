package com.quattage.mechano.foundation.behavior.options;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class ModeOptionBehaviour<E extends Enum<E> & MechanoIconOptionable> extends ScrollValueBehaviour {

	private E[] options;

	public ModeOptionBehaviour(Class<E> enum_, Component label, SmartBlockEntity be, ValueBoxTransform slot) {
		super(label, be, slot);
		options = enum_.getEnumConstants();
		between(0, options.length - 1);
	}

	MechanoIconOptionable getIconForSelected() {
		return get();
	}

	public E get() {
		return options[value];
	}

	@Override
	public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
		return new ValueSettingsBoard(label, max, 1, ImmutableList.of(Component.literal("Mode")),
			new ModeOptionSettingsFormatter(options));
	}
	
	@Override
	public String getClipboardKey() {
		return options[0].getClass().getSimpleName();
	}
}
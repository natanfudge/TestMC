package io.github.natanfudge;

import io.github.natanfudge.impl.events.TitleScreenLoadedEvent;
import io.github.natanfudge.impl.utils.TestLock;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class TestMC implements ModInitializer {

	@Override
	public void onInitialize() {
		//TODO: this might not get called in a case a mod crashes, more testing is required.
		TitleScreenLoadedEvent.EVENT.register(() -> {
			Object testResultHolder = TestLock.getInstance();
			synchronized (testResultHolder){
				testResultHolder.notify();
			}
		});



		System.out.println("ALO MOD EXISTS");
		Block block = new Block(AbstractBlock.Settings.of(Material.BAMBOO)){
			@Override
			public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
				throw new NullPointerException();
			}
		};

		BlockItem item = new BlockItem(block, new Item.Settings());

		Registry.register(Registry.BLOCK,new Identifier("crashmod","crashblock"), block);
		Registry.register(Registry.ITEM,new Identifier("crashmod","crashblock"), item);

		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		System.out.println("Hello Fabric world!");
//		throw new NullPointerException();
	}
}

package io.github.natanfudge.impl;

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
		checkIfTitleScreenLoaded();

		addTestBlock();

		System.out.println("Hello Fabric world!");
//		throw new NullPointerException();
	}

	private void checkIfTitleScreenLoaded() {
		TitleScreenLoadedEvent.EVENT.register(() -> {
			//TODO: this should be more customizable

			// Signal the test method that it needs to move on, the test needs to end.
			Object testResultHolder = TestLock.getInstance();
			synchronized (testResultHolder){
				testResultHolder.notify();
			}
		});
	}

	private void addTestBlock() {
		Block block = new Block(AbstractBlock.Settings.of(Material.BAMBOO)){
			@Override
			public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
				throw new NullPointerException();
			}
		};

		BlockItem item = new BlockItem(block, new Item.Settings());

		Registry.register(Registry.BLOCK,new Identifier("crashmod","crashblock"), block);
		Registry.register(Registry.ITEM,new Identifier("crashmod","crashblock"), item);
	}
}

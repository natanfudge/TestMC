package io.github.natanfudge

import net.fabricmc.api.ModInitializer
import java.lang.NullPointerException
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.block.BlockState
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.ActionResult
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

//TODO: use a proper test mod
/**
 * So how does io.github.natanfudge.TestMC work?
 * TestMc runs Minecraft using devLaunchInjector, as well as some config files in a new thread.
 * When an exception in Minecraft occurs, io.github.natanfudge.TestMC tries its best to make it actually be an unhandled exception, instead of a System.exit(), so it can catch it.
 * This way, when Minecraft throws, it will be caught and transferred to the original test thread, and fail the test.
 * When the test is completed successfully, the lock that keeps the test from ending will be released, marking the test as successful.
 */
internal class TestMC : ModInitializer {
    override fun onInitialize() {
        addTestBlock()

        println("Hello Fabric world!")
//        throw NullPointerException()
    }


    private fun addTestBlock() {
        val block: Block = object : Block(Settings.of(Material.BAMBOO)) {
            override fun onUse(
                state: BlockState,
                world: World,
                pos: BlockPos,
                player: PlayerEntity,
                hand: Hand,
                hit: BlockHitResult
            ): ActionResult {
                throw NullPointerException()
            }
        }
        val item = BlockItem(block, Item.Settings())
        Registry.register(Registry.BLOCK, Identifier("crashmod", "crashblock"), block)
        Registry.register(Registry.ITEM, Identifier("crashmod", "crashblock"), item)
    }

}
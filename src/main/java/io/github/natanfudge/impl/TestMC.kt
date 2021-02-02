package io.github.natanfudge.impl

import net.fabricmc.api.ModInitializer
import java.lang.NullPointerException
import io.github.natanfudge.impl.events.TitleScreenLoadedEvent
import io.github.natanfudge.impl.EndTestException
import net.minecraft.block.AbstractBlock
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

/**
 * So how does TestMC work?
 * TestMc runs Minecraft using devLaunchInjector, as well as some config files.
 * When an exception in Minecraft occurs, TestMC tries its best to make it actually be an unhandled exception, instead of a System.exit(), so it can catch it.
 * This way, when Minecraft throws, it will fail the test.
 * When the test is completed successfully, a [EndTestException] will be thrown, and silently handled, to signal the test run the test is successful.
 */
class TestMC : ModInitializer {
    override fun onInitialize() {
        //TODO: this might not get called in a case a mod crashes, more testing is required.
        checkIfTitleScreenLoaded()
        addTestBlock()
        println("Hello Fabric world!")
//        throw NullPointerException()
    }

    private fun checkIfTitleScreenLoaded() {
        TitleScreenLoadedEvent.EVENT.register(TitleScreenLoadedEvent { throw EndTestException() })
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
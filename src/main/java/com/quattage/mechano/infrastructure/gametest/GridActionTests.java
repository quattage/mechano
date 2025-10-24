package com.quattage.mechano.infrastructure.gametest;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.griddable.SurrogateNode;
import com.quattage.mechano.api.identifier.VoxelUUID;
import com.quattage.mechano.api.landmark.GridNode;
import com.quattage.mechano.api.transmitter.MechanoTransmissionTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class GridActionTests {

    @GameTest(template = "empty", batch="distributionTests")
    public static void voxelGriddables(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.CREATIVE);
        BlockPos startPos = new BlockPos(0, 0, 0);
        BlockPos endPos = new BlockPos(0, 0, 5);
        placeConnector(player, test, startPos);
        placeConnector(player, test, endPos);
        test.runAtTickTime(10, () -> {
            SidedGridDispatcher.server(player).createLink(new VoxelUUID(test.absolutePos(startPos), 0), new VoxelUUID(test.absolutePos(endPos), 0), MechanoTransmissionTypes.HOOKUP);
        });
        test.succeedWhen(() -> {
            ServerGrid grid = SidedGridDispatcher.server(player);
            if(grid == null) test.fail("Couldn't acquire ServerGrid from '" + player.level() + "'");
            GriddableBlockEntity startBE = getBEOrFail(test, startPos, MechanoBlockEntities.CONNECTOR_SINGLE.get());
            GriddableBlockEntity endBE = getBEOrFail(test, endPos, MechanoBlockEntities.CONNECTOR_SINGLE.get());
            SurrogateNode startSurrogate = ((GriddableBlockEntity)startBE).getSurrogate();
            if(startSurrogate == null) test.fail("'" + startBE.getClass().getSimpleName() + "' couldn't supply a surrogate (" + startPos + ")");
            SurrogateNode endSurrogate = ((GriddableBlockEntity)endBE).getSurrogate();
            if(endSurrogate == null) test.fail("'" + endBE.getClass().getSimpleName() + "' couldn't supply a surrogate (" + endPos + ")");
            GridNode startNode = startSurrogate.getSelf();
            if(startNode == null) test.fail("GridNode for " + startBE + " couldn't be acquired (" + startPos + ")");
            GridNode endNode = endSurrogate.getSelf();
            if(endNode == null) test.fail("GridNode for '" + endBE + " couldn't be acquired (" + endPos + ")");
            test.assertTrue(startNode.isLinkedTo(endNode), "Conventional link was not made");
            test.assertTrue(endNode.isLinkedTo(startNode), "Inverted link was not made");
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> T getBEOrFail(GameTestHelper test, BlockPos pos, BlockEntityType<T> expected) {
        BlockEntity be = test.getBlockEntity(pos);
        if(be != null && be.getType() != expected) 
            test.fail("BlockEntity at " + pos + " was of an unexpected type '" + be.getClass().getSimpleName() + "'");
        return (T)be;
    }

    private static void placeConnector(Player player, GameTestHelper test, BlockPos pos) {
        test.setBlock(pos, MechanoBlocks.CONNECTOR_SINGLE.get());
        test.assertBlockPresent(MechanoBlocks.CONNECTOR_SINGLE.get(), pos);
    }
}

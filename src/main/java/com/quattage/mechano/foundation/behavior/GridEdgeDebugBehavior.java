package com.quattage.mechano.foundation.behavior;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.GridClientCache;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.power.features.GIDPair;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Pair;

/***
 * Represents GridClientEdges received by the GridClientCache as in-world lines and boxes for debugging puroses
 */
public class GridEdgeDebugBehavior extends ClientBehavior {

    public GridEdgeDebugBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {
        return instance.options.renderDebug;
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos, double pTicks) {
        
        // for(Map.Entry<SectionPos, List<GridClientEdge>> renderQueue : GridClientCache.INSTANCE.getRenderQueue().entrySet()) {
            
        //     SectionPos section = renderQueue.getKey();
        //     List<GridClientEdge> edgesInSection = renderQueue.getValue();
        //     if(section == null || edgesInSection == null) continue; 

        //     VectorHelper.drawDebugBox(section.center().getCenter());

        //     for(GridClientEdge edge : edgesInSection) {
        //         if(edge != null) {
        //             Pair<Vec3, Vec3> positions = edge.getPositions(world);

        //             if(positions == null) continue;
        //             CreateClient.OUTLINER
        //                 .showLine("edge-" + positions.getFirst() + positions.getSecond(), positions.getFirst(), positions.getSecond())
        //                 .lineWidth(1/16f)
        //                 .disableCull()
        //                 .disableLineNormals();
        //         }
        //     }
        // }

        // Mechano.log("AAA" + GridClientCache.INSTANCE.getAllPaths());

        for(Map.Entry<GIDPair, GID[]> ids : GridClientCache.getInstance().getAllPaths().entrySet()) {
            GID[] steps = ids.getValue();

            for(int x = 0; x < steps.length; x++) {

                Pair<AnchorPoint, WireAnchorBlockEntity> pointA = AnchorPoint.getAnchorAt(world, steps[x]);
                if(pointA == null || pointA.getFirst() == null) continue;

                if(x == 0) VectorHelper.drawDebugBox(pointA.getFirst().getPos());
                else if (x == steps.length - 1) {
                    VectorHelper.drawDebugBox(pointA.getFirst().getPos());
                    continue;
                }

                Pair<AnchorPoint, WireAnchorBlockEntity> pointB = AnchorPoint.getAnchorAt(world, steps[x + 1]);
                if(pointB == null || pointB.getFirst() == null) continue;

                Vec3 from = pointA.getFirst().getPos();
                Vec3 to = pointB.getFirst().getPos();

                CreateClient.OUTLINER
                    .showLine("edge-" + from + "-" + to, from, to)
                    .lineWidth(1/16f)
                    .disableCull()
                    .disableLineNormals();
            }
        }
    }
    
    @Override
    public double setTickRate() {
        return 200;
    }

    @Override
    public double setTickIncrement() {
        return 1;
    }
}   

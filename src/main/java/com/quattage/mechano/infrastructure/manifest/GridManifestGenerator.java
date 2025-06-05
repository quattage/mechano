package com.quattage.mechano.infrastructure.manifest;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBuildParameters;
import com.quattage.mechano.foundation.api.GlobalServerGrid;
import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.NodeIdentifier;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GridManifestGenerator {

    private static final Format DATE_FT = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");

    private Throbber throbber = new Throbber();

    private long requestTime = 0L;
    private long tickTime = 0L;

    private @Nullable CompletableFuture<String> clientResponseTask;
    private @Nullable CompletableFuture<String> compiledManifestTask;

    private @Nullable ServerPlayer requester = null;
    private @Nullable GlobalServerGrid active = null;

    public boolean isQueued() {
        return requester != null && active != null;
    }

    public void tick() {
        if(!isQueued()) return;
        long now = System.currentTimeMillis();
        if(now - tickTime >= 250L) {
            tickTime = now;
            requester.sendSystemMessage(Component.literal("Compiling grid manifest " + throbber.get())
                .withStyle(style -> {
                    return style.withColor(ChatFormatting.AQUA);
                }), true);
        }
    }

    // good luck lmao
    public boolean enqueueRequestFrom(ServerPlayer requester) {
        this.active = SidedGridDispatcher.server(requester);
        this.requester = requester;
        this.requestTime = System.currentTimeMillis();
        if(active.subgrids.isEmpty()) {
            unload();
            return false;
        }
        this.compiledManifestTask = CompletableFuture.supplyAsync(() -> {
            if(!isQueued()) return "";
            String manifest = "▛▙▘▘■  Mechano PowerGrid API manifest generator [" + MechanoBuildParameters.VERSION +  
            "] ■▝▝▟▜\n\n⎙ Requested by: '" + getPlayerName() + "' at [" + getTime() + "]\n⌂ Attached to: '" + getDimensionName() + "'\n\n";

            int count = 0;
            for(PowerGrid grid : active.subgrids) {
                manifest += "⣿ Subgrid " + grid.gridIndex  + ":\n";
                if(grid.nodes.isEmpty()) {
                    manifest += "\t▸ Error (subgrid unpopulated)\n";
                }
                for(GridNode node : grid.nodes.set) {
                    count++;
                    manifest += collectNodeInfo(active.getLevelReader(), node) + "\t└┄┄┄┄\n";
                }
                manifest += "\n--\n";
            }
            manifest += "▙▛▖▖■        Processed " + count + " nodes";
            return manifest;
        }).orTimeout(30L, TimeUnit.SECONDS).whenComplete((result, ex) -> {
            if(ex != null) {
                if(ex instanceof TimeoutException) {
                    requester.sendSystemMessage(Component.literal("Grid manifest request timed out. ⚠")
                    .withStyle(ChatFormatting.RED), true);
                } else {
                    requester.sendSystemMessage(Component.literal("Grid manifest returned an error (See console for details) ⚠")
                    .withStyle(ChatFormatting.RED), true);
                    ex.printStackTrace();
                }
                unload();
                return;
            }

            requester.sendSystemMessage(Component.literal(""), true);
            requester.sendSystemMessage(Component.literal("Manifest saved!")
                .withStyle(ChatFormatting.GREEN), false);
            float elapsed = (System.currentTimeMillis() - this.requestTime) / 1000;
            result += " in " + elapsed + " seconds.         ■▗▗▜▟";
            unload();

            CatnipServices.NETWORK.sendToClient(requester, new ManifestResultPacket("\n" + result));
            Mechano.LOGGER.info("\n\n\n" + result);
        });
        return true;
    }




    private String collectNodeInfo(LevelReader world, GridNode node) {

        String out = "\t┌ Node " + node.toString() + ":  ";
        out += "\n\t┆\t" + (node.isValid() ? "☑ Valid" : "☒ Invalid (See below for details)");
        out += "\n\t┆\t▸ Owned by Grid " + node.owner.gridIndex;
        out += "\n\t┆\t▸ Bound to: ";
        PowerGridBlockEntity host = node.host;

        boolean reaq = false;
        if(host == null) {
            host = node.getHost(world);
            reaq = true;
        }

        if(node.owner != null) {
            if(reaq) out += "(♻) ";
            BlockState state = node.host.getBlockState();
            if(state == null) {
                out += "Error (got null BlockState for '" + node.getClass().getSimpleName() + "')";
            } else {
                Block block = state.getBlock();
                if(block == null) {
                    out += "Error (got null Block for '" + node.getClass().getSimpleName() + "')";
                } else 
                    out += "'" + block.getName().getString() + "'";
            }
        } else {
            out += "N/A - host invalidated, couldn't be reacquired. \n\t\t\t No further information available.";
            return out;
        }

        out += "\n\t┆\t⌕ Dispatch: ";
        out += "\n\t┆\t\t▸ Server Status: " + (host.surrogate.isSynced() ? ("Synced to Subgrid " + host.surrogate.owner.gridIndex) : "no accelerated reference");
        out += "\n\t┆\t\t▸ Client Status: " + requestClientInfoFrom(node.strip());
        out += "\n\t┆\t☍ Links:";

        if(node.links.isEmpty()) {
            out += "\n\t┆\t\t⚠ Error (no links)";
        } else {
            for(GridLink link : node.links) {
                if(!link.startsWith(node)) {
                    out += "⚠ Error (unmatched source)\n";
                    continue;
                }
                ResourceLocation trnsKey = null;
                try { trnsKey = MechanoTransmissionTypes.REGISTRY.getKey(link.transmitter.getType()); }
                catch(Exception e) { trnsKey = Mechano.asResource("transmitter_acquisition_error"); };
                out += "\n\t┆\t\t↪ '" + trnsKey.toString()  + "' to " + link.getEnd().strip();
            }
        }

        return out + "\n";
    }

    private void unload() {
        this.requester = null;
        this.active = null;
        this.requestTime = 0L;
        this.clientResponseTask = null;
        this.compiledManifestTask = null;
    }

    @Override
    public String toString() {
        return requester == null ? "no incoming requests" : "Currently processing a request from '" + getPlayerName() + "' in '" + getDimensionName() + "' at " + getTime();
    }

    private String getPlayerName() {
        return requester == null ? "none" : requester.getDisplayName().getString();
    }

    private String getDimensionName() {
        return active == null ? "none" : active.getWorld().dimension().location().toString();
    }

    private String getTime() {
        return DATE_FT.format(new Date(requestTime));
    }


    private String requestClientInfoFrom(NodeIdentifier.Key addr) {
        if(requester == null) return "Error (manifest)";
        clientResponseTask = new CompletableFuture<>();
        CatnipServices.NETWORK.sendToClient(requester, new ManifestRequestPacket(addr));
        try {
            return clientResponseTask.get(5, TimeUnit.SECONDS);
        } catch(TimeoutException e) { 
            return "Error (timeout)";
        } catch(InterruptedException e) {
            return "Error (interrupted)";
        } catch(Exception e) {
            return "Error (generic)";
        }
    }

    public void handleResponse(String data) {
        if(clientResponseTask != null)
            clientResponseTask.complete(data);
    }



    // hehehhahehhehahehehahehaheheahhehehehehehah
    protected static class Throbber {

        private int index = -1;
        private static final String chars = " ▁▂▃▅▆▇▆▅▃▂▁";

        protected char get() {
            index++;
            if(index >= chars.length()) index = 0;
            return chars.charAt(index);
        }
    }
}

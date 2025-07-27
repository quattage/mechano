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
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;

public class GridManifestGenerator {

    private static final Format DATE_FT = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");

    private Throbber throbber = new Throbber();
    private long requestTime = 0L;
    private long tickTime = 0L;

    private @Nullable CompletableFuture<String> clientResponseTask;
    private @Nullable CompletableFuture<String> compiledManifestTask;
    private @Nullable ServerPlayer requester = null;
    private @Nullable ServerGrid active = null;

    public boolean isQueued() {
        return requester != null && active != null;
    }

    public void tick() {
        if(!isQueued()) return;
        long now = System.currentTimeMillis();
        if(now - tickTime >= 250L) {
            tickTime = now;
            requester.sendSystemMessage(Component.literal("Compiling grid manifest " + throbber.get())
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)), true);
        }
    }

    // good luck lmao
    public boolean enqueueRequestFrom(ServerPlayer requester) {
        this.active = SidedGridDispatcher.server(requester);
        this.requester = requester;
        this.requestTime = System.currentTimeMillis();
        if(active.matrices.isEmpty()) {
            unload();
            return false;
        }
        this.compiledManifestTask = CompletableFuture.supplyAsync(() -> {
            if(!isQueued()) return "";
            String manifest = "▛▙▘▘■   Mechano GridAPI manifest generator [" + MechanoBuildParameters.VERSION +  
            "]   ■▝▝▟▜\n\n⎙ Requested by: '" + getPlayerName() + "' at [" + getTime() + "]\n⌂ Attached to: '" + getDimensionName() + "'\n\n";

            int count = 0;
            for(ServerMatrix grid : active.matrices) {
                manifest += "⣿ Matrix " + grid.getIndex()  + ":\n";
                if(grid.nodes.isEmpty()) {
                    manifest += "\t▸ Error (matrix unpopulated)\n";
                }
                for(GridNode node : grid.nodes) {
                    count++;
                    manifest += collectNodeInfo(active.getWorld(), node) + "\t└┄┄┄┄\n";
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

        String out = "\t┌ ▣ " + node.getAddress().toString(world) + ":  ";
        out += "\n\t┆\t" + (node.hasLinks() ? "☑ Valid" : "☒ Invalid (See below for details)");
        out += "\n\t┆\t▸ Owned by Matrix " + node.getOwner().getIndex();
        Griddable<?> points = node.getGriddable();
        out += "\n\t┆\t▸ Bound to: ";

        if(node.getOwner() != null) {
            String state = points.describeState();
            if(state == null || state.isEmpty())
                out += "Error (Host '" + points + "' has not implemented describeState())";
            else out += state;
        } else {
            out += "N/A - host invalidated, couldn't be reacquired. \n\t\t\t No further information available.";
            return out;
        }

        out += "\n\t┆\t▸ Data scope: " + node.getAddress().describeDataScope(world);
        out += "\n\t┆\t⌕ Dispatch: ";
        out += "\n\t┆\t\t▸ Server Status: " + (points.getSurrogate().isSynced(world) ? ("Synced to Matrix " + points.getSurrogate().getOwnerMatrix().getIndex()) : "no accelerated reference");
        out += "\n\t┆\t\t▸ Client Status: " + requestClientInfoFrom(node.getAddress());
        out += "\n\t┆\t☍ Links:";

        if(node.getLinkCount() <= 0)
            out += "\n\t┆\t\t⚠ Error (no links)";
        else {
            for(GridLink link : node) {
                if(!link.startsWith(node.getAddress())) {
                    out += "\n\t┆\t\t⚠ (unmatched source) [" + link.getStart().toString(world) + "-> " + link.getEnd().toString(world) + "]";
                    continue;
                }
                out += "\n\t┆\t\t↪ '" + link.getTransmitter().getType()  + "' to " + link.getEndNode().getAddress().toString(world);
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

    private String requestClientInfoFrom(GridUUID addr) {
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

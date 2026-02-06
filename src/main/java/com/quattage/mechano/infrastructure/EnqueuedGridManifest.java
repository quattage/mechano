package com.quattage.mechano.infrastructure;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;

import com.quattage.mechano.MechanoBuildParameters;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.infrastructure.ReflectionWizard.DoNotAnalyze;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@DoNotAnalyze
public class EnqueuedGridManifest {

    private static final String FILE = "dumps\\grid_manifest.log";

    private Throbber throbber;
    private Entity requester;
    private ServerGrid grid;
    private CompletableFuture<String> manifestFuture;
    private String resultString = "";
    private long tickTime = 0L;

    public EnqueuedGridManifest(ServerLevel world, Entity requester) {
        this(Grid.server(world), requester);
    }

    public EnqueuedGridManifest(ServerGrid grid, Entity requester) {
        this.requester = requester;
        this.grid = grid;
        this.manifestFuture = CompletableFuture.supplyAsync(() -> this.resultString = writeManifest())
        .orTimeout(30L, TimeUnit.SECONDS).whenComplete((result, ex) -> {
            if(!(requester instanceof ServerPlayer sp)) return;
            if(ex != null) {
                ex.printStackTrace();
                this.resultString = (ex instanceof TimeoutException ? "Grid manifest request timed out. ⚠" : "Unknown error compiling grid manifest! (See console) ⚠");
                sp.sendSystemMessage(Component.literal(resultString).withStyle(ChatFormatting.RED), true);
                return;
            }
            promptPlayer(sp);
        });
        this.throbber = new Throbber();
    }

    /**
     * Writes a new manifest String for <code>grid</code> and 
     * returns the string immediately without off-thread execution
     * or file writing. This operation will take quite a long time
     * to complete, so don't call this in production code.
     * @param grid Grid to get a manifest for
     * @return A formatted string containing manifest data
     */
    public static String getImmediately(ServerGrid grid) {
        EnqueuedGridManifest result = new EnqueuedGridManifest(grid);
        String output = result.resultString;
        result.consume();
        return output;
    }

    private EnqueuedGridManifest(ServerGrid grid) {
        this.grid = grid;
        this.requester = null;
        this.manifestFuture = null;
        this.throbber = null;
        this.resultString = writeManifest();
    }

    public String writeManifest() {
        StopWatch timer = StopWatch.createStarted();
        String out =  "\n▙▚▘▘\t\t\t\t\t       Mechano GridAPI manifest    \t\t\t\t\t▝▝▞▟\n\n";
            out += ""
                + "API " + MechanoBuildParameters.asString() + "\n"
                + "requested at [" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()) + "]"
                + " by '" + (requester == null ? "n/a" : requester.getName().getString()) + "' in " + grid.getDimensionName() + "\n"
                + "solver method: " + grid.getSolver().describeSelf() + "\n"
                + "lifecycle status: " + grid.getStatusHolder() + "\n"
                + "unsaved changes for this session? " + (grid.getProcessQueue().hasUnsavedChanges() ? "yes" : "no") + "\n"
                + "ground: " + (grid.getCommonGround() == null ? "not yet instantiated" : grid.getCommonGround().hashCode()) + "\n"
                + "memory footprint analysis: " + ReflectionWizard.estimateFootprint(grid) + "\n--\n\n"
                + "indexer: " + grid.indexer().toString() + "\n\n"
                + "netlist:" + grid.netlist().toFullString(grid) + "\n\n"
                + "links:" + grid.lookup().toString() + "\n\n"
                + "--\n"
                + "  ♨ github.com/quattage/mechano\n"
                + "  ☎ discord.gg/85ufgRwy2g\n"
                + "--\n";
        return out += "\n▛▞▖▖\t\t\t\t       Manifest generated in " + DurationFormatUtils.formatDuration(timer.getTime(), "ss.SSS") + "s    \t\t\t\t▗▗▚▜ \n";
    }

    public void tick() {
        if(isConsumed()) return;
        if(hasResult() || manifestFuture.isDone() || manifestFuture.isCancelled()) {
            save();
            consume();
            return;
        }
        if(requester instanceof ServerPlayer sp)
            showThrobber(sp);
    }

    private void showThrobber(ServerPlayer sp) {
        long now = System.currentTimeMillis();
        if(now - tickTime >= 150L) {
            tickTime = now;
            sp.sendSystemMessage(Component.literal("Compiling grid manifest " + throbber.throb())
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)), true);
        }
    }

    public boolean isConsumed() {
        return requester == null || grid == null || manifestFuture == null;
    }

    public boolean hasResult() {
        return !isConsumed() && !(resultString == null || resultString.isEmpty());
    }

    public void save() {
        String directory = getDirectory();
        File output = new File(directory);
        try(PrintWriter pw = new PrintWriter(output)) {
            pw.print(resultString);
            pw.close();
        } catch (Exception e) {}
    }

    private String getDirectory() {
        String abspath = Minecraft.getInstance().gameDirectory.getAbsolutePath();
        return abspath.substring(0, abspath.length() - 1) + EnqueuedGridManifest.FILE;
    }

    public void consume() {
        this.throbber = null;
        this.requester = null;
        this.grid = null;
        this.manifestFuture = null;
        this.resultString = null;
        tickTime = 0;
    }

    private void promptPlayer(ServerPlayer sp) {
        sp.sendSystemMessage(Component.literal(""), true);
            sp.sendSystemMessage(Component.literal("Manifest saved to")
                .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" [").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withBold(true)))
                    .append(Component.literal(getDirectory())
                        .withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, getDirectory()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))
                        ))
                    .append(Component.literal("] ").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withBold(true)))
                , false);
    }

    public static class Throbber {

        private int index = -1;
        private static final String chars = " ▁▂▃▅▆▇▆▅▃▂▁";

        protected char throb() {
            index++;
            if(index >= Throbber.chars.length()) index = 0;
            return Throbber.chars.charAt(index);
        }
    }
}

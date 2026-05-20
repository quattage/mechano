package com.quattage.mechano.infrastructure;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Objects;

import org.slf4j.Logger;

import net.minecraft.client.Minecraft;

public class ActionLog {

    protected String text = "";
    private boolean enabled = false;

    public static String makeFileLocation(String name) {
        String abspath = Minecraft.getInstance().gameDirectory.getAbsolutePath();
        return abspath.substring(0, abspath.length() - 1) + name;
    }

    public ActionLog enableOnto(Object obj) {
        Objects.requireNonNull(obj);
        enable();
        line("Beginning action log for " + obj.getClass().getSimpleName() + " @" + obj.hashCode() + " at ").addTime();
        line("");
        return this;
    }

    public ActionLog addTime() {
        text += "" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis());
        return this;
    }

    public ActionLog lineTime() {
        text += "\n" + addTime();
        return this;
    }

    public ActionLog line(String text) {
        if(!enabled || text == null) return this;
        this.text += "\n" + text;
        return this;
    }

    public ActionLog add(String text) {
        if(!enabled || text == null) return this;
        this.text += text;
        return this;
    }

    public ActionLog line(Object obj) {
        if(!enabled) return this;
        this.text += "\n" + obj == null ? "null" : obj.toString();
        return this;
    }

    public ActionLog add(Object obj) {
        if(!enabled) return this;
        this.text += obj == null ? "null" : obj.toString();
        return this;
    }

    public ActionLog divider() {
        if(!enabled) return this;
        text += "\n--\n";
        return this;
    }

    public ActionLog enable() {
        enabled = true;
        return this;
    }

    public ActionLog disable() {
        enabled = false;
        return this;
    }

    public ActionLog setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public ActionLog clear() {
        this.text = "";
        this.enabled = false;
        return this;
    }

    public boolean isBlank() {
        return text.isBlank();
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ActionLog that)) return false;
        return this.text == that.text;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    public void save() { save(ActionLog.makeFileLocation("manifest.log")); }
    public void save(String directory) {
        if(isBlank()) return;
        File output = new File(directory);
        try(PrintWriter pw = new PrintWriter(output)) {
            pw.print(text);
            pw.close();
        } catch (Exception e) {}
    }

    public void log(Logger logger) {
        Objects.requireNonNull(logger);
        logger.info("::::\n" + text + "\n");
    }
}

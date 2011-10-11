package de.codeinfection.quickwango.Announcer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;

public class Announcer extends JavaPlugin
{
    protected static final Logger logger = Logger.getLogger("Minecraft");
    public static boolean debugMode = false;
    
    protected Server server;
    protected PluginManager pm;
    protected Configuration config;
    protected BukkitScheduler scheduler;
    protected File dataFolder;

    protected boolean instantStart = false;

    public void onEnable()
    {
        this.server = this.getServer();
        this.pm = this.server.getPluginManager();
        this.config = this.getConfiguration();
        this.scheduler = this.server.getScheduler();
        this.dataFolder = this.getDataFolder();

        this.dataFolder.mkdirs();
        // Create default config if it doesn't exist.
        if (!(new File(this.dataFolder, "config.yml")).exists())
        {
            this.defaultConfig();
        }
        this.loadConfig();

        debugMode = this.config.getBoolean("debug", debugMode);
        this.instantStart = this.config.getBoolean("instantStart", this.instantStart);
        Pattern pattern = Pattern.compile("^(\\d+)([tsmhd])?$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(this.config.getString("interval", "15"));
        matcher.find();
        int interval = 0;
        try
        {
            interval = Integer.valueOf(String.valueOf(matcher.group(1)));
        }
        catch (NumberFormatException e)
        {
            error("The given interval was invalid!", e);
            return;
        }
        String unitSuffix = matcher.group(2);
        if (unitSuffix == null)
        {
            unitSuffix = "m";
        }
        switch (unitSuffix.toLowerCase().charAt(0))
        {
            case 'd':
                interval *= 24;
            case 'h':
                interval *= 60;
            case 'm':
                interval *= 60;
            case 's':
                interval *= 20;
        }

        debug("Calculated a interval of " + interval + " ticks");

        this.getCommand("announce").setExecutor(new AnnounceCommand(this.server, this.dataFolder));
        this.getCommand("reloadannouncer").setExecutor(new ReloadannouncerCommand(this));

        try
        {
            AnnouncerTask task = new AnnouncerTask(server, dataFolder);

            debug("Start instantly? - " + String.valueOf(this.instantStart));
            if (this.scheduler.scheduleAsyncRepeatingTask(this, task, (this.instantStart ? 0 : interval), interval) < 0)
            {
                error("Failed to schedule the announcer task!");
                return;
            }
        }
        catch (AnnouncementLoadException e)
        {
            error("No announcements found!");
            return;
        }

        System.out.println(this.getDescription().getName() + " (v" + this.getDescription().getVersion() + ") enabled");
    }

    public void onDisable()
    {
        this.scheduler.cancelTasks(this);
        System.out.println(this.getDescription().getName() + " Disabled");
    }

    private void loadConfig()
    {
        this.config.load();
    }

    private void defaultConfig()
    {
        this.config.setProperty("interval", "15");
        this.config.setProperty("instantStart", this.instantStart);
        this.config.setProperty("debug", debugMode);

        this.config.save();
    }

    public static void log(Level logLevel, String msg, Throwable t)
    {
        logger.log(logLevel, "[Announcer] " + msg, t);
    }

    public static void log(Level logLevel, String msg)
    {
        log(logLevel, msg, null);
    }

    public static void log(String msg)
    {
        log(Level.INFO, msg);
    }

    public static void error(String msg)
    {
        log(Level.SEVERE, msg);
    }

    public static void error(String msg, Throwable t)
    {
        log(Level.SEVERE, msg, t);
    }

    public static void debug(String msg)
    {
        if (debugMode)
        {
            log("[debug] " + msg);
        }
    }

    public static List<String> loadAnnouncement(File file) throws AnnouncementLoadException
    {
        List<String> lines = new ArrayList<String>();
        if (!file.exists())
        {
            throw new AnnouncementLoadException("Announcement does not exist", 1);
        }
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null)
            {
                lines.add(line.trim().replaceAll("&([a-f0-9])", "\u00A7$1"));
            }
            reader.close();
        }
        catch (IOException e)
        {
            throw new AnnouncementLoadException("IOException: " + e.getLocalizedMessage(), e, 2);
        }
        return lines;
    }
}

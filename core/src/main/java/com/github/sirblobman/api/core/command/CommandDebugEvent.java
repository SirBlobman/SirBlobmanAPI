package com.github.sirblobman.api.core.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import com.github.sirblobman.api.command.ConsoleCommand;
import com.github.sirblobman.api.core.CorePlugin;

public class CommandDebugEvent extends ConsoleCommand {
    private final CorePlugin plugin;
    public CommandDebugEvent(CorePlugin plugin) {
        super(plugin, "debug-event");
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(ConsoleCommandSender sender, String[] args) {
        if(args.length == 1) {
            List<Class<?>> exampleClassList = Arrays.asList(PlayerInteractEvent.class, PlayerTeleportEvent.class, EntitySpawnEvent.class);
            List<String> valueList = exampleClassList.stream().map(Class::getName).collect(Collectors.toList());
            return getMatching(valueList, args[0]);
        }

        return Collections.emptyList();
    }

    @Override
    public boolean execute(ConsoleCommandSender sender, String[] args) {
        if(args.length < 1) return false;

        try {
            String className = args[0];
            Class<?> namedClass = Class.forName(className);
            Class<? extends Event> eventClass = namedClass.asSubclass(Event.class);

            Method method_getHandlerList = eventClass.getMethod("getHandlerList");
            Map<EventPriority, Map<String, List<String>>> eventMap = new EnumMap<>(EventPriority.class);
            HandlerList handlerList = (HandlerList) method_getHandlerList.invoke(null);

            RegisteredListener[] registeredListenerArray = handlerList.getRegisteredListeners();
            for(RegisteredListener registeredListener : registeredListenerArray) {
                EventPriority priority = registeredListener.getPriority();
                Map<String, List<String>> pluginListenerClassMap = eventMap.getOrDefault(priority, new LinkedHashMap<>());

                Plugin plugin = registeredListener.getPlugin();
                String pluginName = plugin.getName();
                List<String> classNameList = pluginListenerClassMap.getOrDefault(pluginName, new ArrayList<>());

                Listener listener = registeredListener.getListener();
                Class<? extends Listener> listenerClass = listener.getClass();
                String listenerClassName = listenerClass.getName();
                classNameList.add(listenerClassName);

                pluginListenerClassMap.put(pluginName, classNameList);
                eventMap.put(priority, pluginListenerClassMap);
            }

            Logger logger = this.plugin.getLogger();
            logger.info(" ");
            logger.info("Debugging Event: " + eventClass.getName());

            Set<Entry<EventPriority, Map<String, List<String>>>> entrySet = eventMap.entrySet();
            for(Entry<EventPriority, Map<String, List<String>>> entry : entrySet) {
                EventPriority priority = entry.getKey();
                logger.info("  " + priority + ":");

                Map<String, List<String>> pluginListenerClassMap = entry.getValue();
                Set<Entry<String, List<String>>> entrySet2 = pluginListenerClassMap.entrySet();
                for(Entry<String, List<String>> entry2 : entrySet2) {
                    String pluginName = entry2.getKey();
                    logger.info("    " + pluginName + ":");

                    List<String> classNameList = entry2.getValue();
                    for(String listenerClassName : classNameList) {
                        logger.info("    - " + listenerClassName);
                    }
                }
            }

            return true;
        } catch(Exception ex) {
            String errorMessage = ex.getMessage();
            sender.sendMessage(ChatColor.RED + "An error occurred while debugging an event, check console for further details:");
            sender.sendMessage(ChatColor.RED + "  " + errorMessage);

            Logger logger = this.plugin.getLogger();
            logger.log(Level.WARNING, "An error occurred while debugging an event:", ex);
            return true;
        }
    }
}

package com.github.sirblobman.api.nms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.github.sirblobman.api.utility.ItemUtility;

import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.item.ItemStack;

public class ItemHandler_1_17_R1 extends ItemHandler {
    public ItemHandler_1_17_R1(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getLocalizedName(org.bukkit.inventory.ItemStack item) {
        if(ItemUtility.isAir(item)) {
            return "Air";
        }

        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta != null && itemMeta.hasDisplayName()) {
            return itemMeta.getDisplayName();
        }

        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        return nmsItem.getName().getText();
    }

    @Override
    public String getKeyString(org.bukkit.inventory.ItemStack item) {
        if(ItemUtility.isAir(item)) {
            return "minecraft:air";
        }

        Material bukkitMaterial = item.getType();
        NamespacedKey bukkitMaterialKey = bukkitMaterial.getKey();
        return bukkitMaterialKey.toString();
    }

    @Override
    public String toNBT(org.bukkit.inventory.ItemStack item) {
        NBTTagCompound nbtData = new NBTTagCompound();
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

        nmsItem.save(nbtData);
        return nbtData.toString();
    }

    @Override
    public org.bukkit.inventory.ItemStack fromNBT(String string) {
        try {
            NBTTagCompound nbtData = MojangsonParser.parse(string);
            ItemStack nmsItem = ItemStack.a(nbtData);
            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch(CommandSyntaxException ex) {
            JavaPlugin plugin = getPlugin();
            Logger logger = plugin.getLogger();
            logger.log(Level.WARNING, "Failed to parse an NBT string to an item, returning AIR...", ex);
            return new org.bukkit.inventory.ItemStack(Material.AIR);
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack setCustomNBT(org.bukkit.inventory.ItemStack item, String key, String value) {
        if(item == null || key == null || key.isEmpty() || value == null) return item;

        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return item;

        JavaPlugin plugin = getPlugin();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
        persistentDataContainer.set(namespacedKey, PersistentDataType.STRING, value);

        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    public String getCustomNBT(org.bukkit.inventory.ItemStack item, String key, String defaultValue) {
        if(item == null || key == null || key.isEmpty()) return defaultValue;

        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return defaultValue;

        JavaPlugin plugin = getPlugin();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
        return persistentDataContainer.getOrDefault(namespacedKey, PersistentDataType.STRING, defaultValue);
    }

    @Override
    public org.bukkit.inventory.ItemStack removeCustomNBT(org.bukkit.inventory.ItemStack item, String key) {
        if(item == null || key == null || key.isEmpty()) return item;

        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return item;

        JavaPlugin plugin = getPlugin();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
        persistentDataContainer.remove(namespacedKey);

        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    public org.bukkit.inventory.ItemStack fromBase64String(String string) {
        if (string == null || string.isEmpty()) return null;

        NBTTagCompound nbtTagCompound;
        byte[] decode = Base64.getDecoder().decode(string);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decode);

        try {
            nbtTagCompound = NBTCompressedStreamTools.a(byteArrayInputStream);
        } catch (Exception ex) {
            Logger logger = getPlugin().getLogger();
            logger.log(Level.WARNING, "Failed to decode an item from a string because an error occurred:", ex);
            return null;
        }

        ItemStack nmsItem = ItemStack.a(nbtTagCompound);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public String toBase64String(org.bukkit.inventory.ItemStack item) {
        if (ItemUtility.isAir(item)) return null;

        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        CraftItemStack.asNMSCopy(item).save(nbtTagCompound);

        try {
            NBTCompressedStreamTools.a(nbtTagCompound, byteArrayOutputStream);
        } catch (Exception ex) {
            Logger logger = getPlugin().getLogger();
            logger.log(Level.WARNING, "Failed to encode an item to a string because an error occurred:", ex);
            return null;
        }

        byte[] encode = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(encode);
    }
}

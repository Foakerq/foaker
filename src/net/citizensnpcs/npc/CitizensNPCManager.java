package net.citizensnpcs.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCManager;
import net.citizensnpcs.api.npc.trait.Character;
import net.citizensnpcs.api.npc.trait.trait.SpawnLocation;
import net.citizensnpcs.resource.lib.CraftNPC;
import net.citizensnpcs.storage.Storage;
import net.citizensnpcs.util.ByIdArray;
import net.minecraft.server.ItemInWorldManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Packet29DestroyEntity;
import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CitizensNPCManager implements NPCManager {
    private final ByIdArray<NPC> spawned = new ByIdArray<NPC>();
    private final ByIdArray<NPC> byID = new ByIdArray<NPC>();
    private final Map<Integer, Set<String>> selected = new ConcurrentHashMap<Integer, Set<String>>();
    private final Storage saves;

    public CitizensNPCManager(Storage saves) {
        this.saves = saves;
    }

    @Override
    public NPC createNPC(String name) {
        return createNPC(name, null);
    }

    @Override
    public NPC createNPC(String name, Character character) {
        return createNPC(generateUniqueId(), name, character);
    }

    public NPC createNPC(int id, String name, Character character) {
        CitizensNPC npc = new CitizensNPC(this, id, name);
        npc.setCharacter(character);
        byID.put(npc.getId(), npc);
        return npc;
    }

    public void despawn(NPC npc) {
        CraftNPC mcEntity = ((CitizensNPC) npc).getHandle();
        Location loc = npc.getBukkitEntity().getLocation();
        npc.getTrait(SpawnLocation.class).setLocation(loc);

        if (selected.get(npc.getId()) != null)
            selected.get(npc.getId()).clear();
        spawned.remove(mcEntity.getPlayer().getEntityId());
        for (Player player : Bukkit.getOnlinePlayers())
            ((CraftPlayer) player).getHandle().netServerHandler.sendPacket(new Packet29DestroyEntity(mcEntity.id));
        getWorldServer(loc.getWorld()).removeEntity(mcEntity);
        mcEntity.die();
    }

    @Override
    public Iterator<NPC> iterator() {
        return byID.iterator();
    }

    private MinecraftServer getMinecraftServer(Server server) {
        return ((CraftServer) server).getServer();
    }

    @Override
    public NPC getNPC(Entity entity) {
        return spawned.get(entity.getEntityId());
    }

    @Override
    public NPC getNPC(int id) {
        return byID.get(id);
    }

    @Override
    public Collection<NPC> getNPCs(Class<? extends Character> character) {
        List<NPC> npcs = new ArrayList<NPC>();
        for (NPC npc : this) {
            if (npc.getCharacter() != null
                    && CitizensAPI.getCharacterManager().getInstance(npc.getCharacter().getName(), npc) != null)
                npcs.add(npc);
        }
        return npcs;
    }

    private int generateUniqueId() {
        int count = 0;
        while (getNPC(count++) != null)
            ;
        return count - 1;
    }

    private WorldServer getWorldServer(World world) {
        return ((CraftWorld) world).getHandle();
    }

    @Override
    public boolean isNPC(Entity entity) {
        return spawned.contains(entity.getEntityId());
    }

    public void remove(NPC npc) {
        if (spawned.contains(((CitizensNPC) npc).getHandle().getPlayer().getEntityId()))
            despawn(npc);
        byID.remove(npc.getId());
        saves.getKey("npc").removeKey("" + npc.getId());
        selected.remove(npc.getId());
    }

    public CraftNPC spawn(NPC npc, Location loc) {
        WorldServer ws = getWorldServer(loc.getWorld());
        CraftNPC mcEntity = new CraftNPC(getMinecraftServer(ws.getServer()), ws, npc.getFullName(),
                new ItemInWorldManager(ws));
        mcEntity.removeFromPlayerMap(npc.getFullName());
        mcEntity.setPositionRotation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        ws.addEntity(mcEntity);
        ws.players.remove(mcEntity);

        spawned.put(mcEntity.getPlayer().getEntityId(), npc);
        return mcEntity;
    }

    public void selectNPC(Player player, NPC npc) {
        // Remove existing selection if any
        NPC select = getSelectedNPC(player);
        if (select != null)
            selected.get(select.getId()).remove(player.getName());

        Set<String> selectors = selected.get(npc.getId());
        if (selectors == null)
            selectors = new HashSet<String>();
        selectors.add(player.getName());
        selected.put(npc.getId(), selectors);
    }

    public boolean npcIsSelectedByPlayer(Player player, NPC npc) {
        if (!selected.containsKey(npc.getId()))
            return false;
        return selected.get(npc.getId()).contains(player.getName());
    }

    public NPC getSelectedNPC(Player player) {
        for (int id : selected.keySet()) {
            if (selected.get(id).contains(player.getName()))
                return getNPC(id);
        }
        return null;
    }

    public int size() {
        return byID.size();
    }
}
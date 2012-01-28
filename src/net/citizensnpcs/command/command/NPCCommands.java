package net.citizensnpcs.command.command;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.trait.Character;
import net.citizensnpcs.api.npc.trait.DefaultInstanceFactory;
import net.citizensnpcs.api.npc.trait.trait.Owner;
import net.citizensnpcs.command.CommandContext;
import net.citizensnpcs.command.annotation.Command;
import net.citizensnpcs.command.annotation.Permission;
import net.citizensnpcs.command.annotation.Requirements;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.CitizensNPCManager;
import net.citizensnpcs.util.Messaging;
import net.citizensnpcs.util.StringHelper;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Requirements(selected = true, ownership = true)
public class NPCCommands {
    private final CitizensNPCManager npcManager;
    private final DefaultInstanceFactory<Character> characterManager;

    public NPCCommands(CitizensNPCManager npcManager, DefaultInstanceFactory<Character> characterManager) {
        this.npcManager = npcManager;
        this.characterManager = characterManager;
    }

    @Command(
             aliases = { "npc" },
             usage = "create [name] (character)",
             desc = "Create a new NPC",
             modifiers = { "create" },
             min = 2,
             max = 3)
    @Permission("npc.create")
    @Requirements
    public void createNPC(CommandContext args, Player player, NPC npc) {
        CitizensNPC create = (CitizensNPC) npcManager.createNPC(args.getString(1));
        String successMsg = ChatColor.GREEN + "You created " + StringHelper.wrap(create.getName());
        boolean success = true;
        if (args.argsLength() == 3) {
            if (characterManager.getInstance(args.getString(2), create) == null) {
                Messaging.sendError(player,
                        "The character '" + args.getString(2) + "' does not exist. " + create.getName()
                                + " was created at your location without a character.");
                success = false;
            } else {
                create.setCharacter(characterManager.getInstance(args.getString(2), create));
                successMsg += " with the character " + StringHelper.wrap(args.getString(2));
            }
        }
        successMsg += " at your location.";

        // Set the owner
        create.addTrait(new Owner(player.getName()));
        create.getTrait(Owner.class).setOwner(player.getName());

        create.spawn(player.getLocation());
        npcManager.selectNPC(player, create);
        if (success)
            Messaging.send(player, successMsg);
    }

    @Command(
             aliases = { "npc" },
             usage = "spawn [id]",
             desc = "Spawn an existing NPC",
             modifiers = { "spawn" },
             min = 2,
             max = 2)
    @Permission("npc.spawn")
    @Requirements
    public void spawnNPC(CommandContext args, Player player, NPC npc) {
        CitizensNPC respawn = (CitizensNPC) npcManager.getNPC(args.getInteger(1));
        if (respawn == null) {
            Messaging.sendError(player, "No NPC with the ID '" + args.getInteger(1) + "' exists.");
            return;
        }

        if (!respawn.getTrait(Owner.class).getOwner().equals(player.getName())) {
            Messaging.sendError(player, "You must be the owner of this NPC to execute that command.");
            return;
        }

        if (respawn.spawn(player.getLocation())) {
            npcManager.selectNPC(player, respawn);
            Messaging.send(player, ChatColor.GREEN + "You respawned " + StringHelper.wrap(respawn.getName())
                    + " at your location.");
        } else
            Messaging.sendError(player, respawn.getName()
                    + " is already spawned at another location. Use '/npc tp' to teleport the NPC to your location.");
    }

    @Command(
             aliases = { "npc" },
             usage = "despawn",
             desc = "Despawn an NPC",
             modifiers = { "despawn" },
             min = 1,
             max = 1)
    @Permission("npc.despawn")
    public void despawnNPC(CommandContext args, Player player, NPC npc) {
        npc.despawn();
        Messaging.send(player, ChatColor.GREEN + "You despawned " + StringHelper.wrap(npc.getName()) + ".");
    }
}
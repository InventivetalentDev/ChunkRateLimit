package org.inventivetalent.chunkratelimit;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DelayedChunk {

	public final UUID            player;
	public final PacketContainer packet;
	public final int x;
	public final int z;

	public DelayedChunk(Player player, PacketContainer packet) {
		this.player = player.getUniqueId();
		this.packet = packet;
		StructureModifier<Integer> ints = packet.getIntegers();
		this.x = ints.read(0);
		this.z = ints.read(1);
	}

	public int distance() {
		Chunk chunk = getPlayer().getLocation().getChunk();
		return ((this.x - chunk.getX()) * (this.x - chunk.getX())) + ((this.z - chunk.getZ()) * (this.z - chunk.getZ()));
	}

	public Player getPlayer() {
		return Bukkit.getPlayer(this.player);
	}

	public String getId() {
		return this.player.toString() + "x" + this.x + "z" + this.z;
	}
}

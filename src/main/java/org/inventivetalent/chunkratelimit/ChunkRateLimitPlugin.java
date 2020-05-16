package org.inventivetalent.chunkratelimit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class ChunkRateLimitPlugin extends JavaPlugin {

	private ProtocolManager protocolManager;

	private int                 chunkCounter  = 0;
	private Deque<DelayedChunk> delayedChunks = new ArrayDeque<>();
	private Set<String>         processing    = new HashSet<>();

	private int sendInterval       = 10;
	private int sendEach           = 1;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		sendInterval = getConfig().getInt("sendInterval", sendInterval);
		sendEach = getConfig().getInt("sendEach", sendEach);

		protocolManager = ProtocolLibrary.getProtocolManager();

		protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.MAP_CHUNK) {
			@Override
			public void onPacketSending(PacketEvent event) {
				chunkCounter++;
				if (chunkCounter > 2) {
					DelayedChunk chunk = new DelayedChunk(event.getPlayer(), event.getPacket());
					String chunkId = chunk.getId();
					if (!processing.contains(chunkId)) {
						delayedChunks.add(chunk);
						event.setCancelled(true);
					} else {
						processing.remove(chunkId);
					}
				}
			}
		});

		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < sendEach; i++) {
					if (chunkCounter > 0) { chunkCounter--; }
					if (delayedChunks.size() <= 0) { return; }
					DelayedChunk chunk = delayedChunks.pop();
					if (chunk != null) {
						processing.add(chunk.getId());
						try {
							protocolManager.sendServerPacket(chunk.getPlayer(), chunk.packet);
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, sendInterval, sendInterval);
	}

	@Override
	public void onDisable() {
		protocolManager.removePacketListeners(this);
	}
}

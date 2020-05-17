package org.inventivetalent.chunkratelimit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ChunkRateLimitPlugin extends JavaPlugin implements Listener {

	private ProtocolManager protocolManager;

	private int                            chunkCounter           = 0;
	private Deque<DelayedChunk>            delayedChunks          = new ArrayDeque<>();
	private Map<UUID, Deque<DelayedChunk>> perPlayerDelayedChunks = new HashMap<>();
	private Set<String>                    processing             = new HashSet<>();

	private int     sendInterval   = 10;
	private int     sendEach       = 1;
	private boolean perPlayerQueue = false;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		sendInterval = getConfig().getInt("sendInterval", sendInterval);
		sendEach = getConfig().getInt("sendEach", sendEach);
		perPlayerQueue = getConfig().getBoolean("perPlayerQueue", perPlayerQueue);

		Bukkit.getPluginManager().registerEvents(this, this);

		protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.MAP_CHUNK) {
			@Override
			public void onPacketSending(PacketEvent event) {
				chunkCounter++;
				if (chunkCounter > 2) {
					Player player = event.getPlayer();
					if (!player.hasPermission("chunkratelimit.bypass")) {
						DelayedChunk chunk = new DelayedChunk(player, event.getPacket());
						String chunkId = chunk.getId();
						if (!processing.contains(chunkId)) {
							if (perPlayerQueue) {
								if (!perPlayerDelayedChunks.containsKey(chunk.player)) {
									perPlayerDelayedChunks.put(chunk.player, new ArrayDeque<>());
								}
								perPlayerDelayedChunks.get(chunk.player).add(chunk);
							} else {
								delayedChunks.add(chunk);
							}
							event.setCancelled(true);
						} else {
							processing.remove(chunkId);
						}
					}
				}
			}
		});

		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < sendEach; i++) {
					DelayedChunk chunk = null;
					Player chunkPlayer = null;
					if (perPlayerQueue){
						if (perPlayerDelayedChunks.isEmpty()) return;
						for (UUID uuid : perPlayerDelayedChunks.keySet()) {
							Deque<DelayedChunk> playerQueue = perPlayerDelayedChunks.get(uuid);
							if(playerQueue.isEmpty())continue;
							if (chunkCounter > 0) { chunkCounter--; }
							chunk = playerQueue.pop();
							chunkPlayer = chunk.getPlayer();
							sendChunk(chunk, chunkPlayer);
						}
					} else {
						if (delayedChunks.isEmpty()) { return; }
						if (chunkCounter > 0) { chunkCounter--; }
						int j = 0;
						while (j++ < 50 && (chunkPlayer == null || !chunkPlayer.isOnline()) && delayedChunks.size() > 0) {
							chunk = delayedChunks.pop();
							chunkPlayer = chunk.getPlayer();
						}
						sendChunk(chunk, chunkPlayer);
					}
				}
			}
		}, sendInterval, sendInterval);
	}

	@EventHandler
	public void on(PlayerQuitEvent event) {
		if (perPlayerQueue) {
			perPlayerDelayedChunks.remove(event.getPlayer().getUniqueId());
		}
	}

	public void sendChunk(DelayedChunk chunk, Player chunkPlayer) {
		if (chunkPlayer != null && chunkPlayer.isOnline()) {
			processing.add(chunk.getId());
			try {
				protocolManager.sendServerPacket(chunkPlayer, chunk.packet);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDisable() {
		protocolManager.removePacketListeners(this);
	}
}

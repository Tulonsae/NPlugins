/***************************************************************************
 * Project file:    NPlugins - NPlayer - LoggedOutUserHandler.java         *
 * Full Class name: fr.ribesg.bukkit.nplayer.user.LoggedOutUserHandler     *
 *                                                                         *
 *                Copyright (c) 2013 Ribesg - www.ribesg.fr                *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.nplayer.user;
import fr.ribesg.bukkit.ncore.common.event.PlayerJoinedEvent;
import fr.ribesg.bukkit.ncore.lang.MessageId;
import fr.ribesg.bukkit.nplayer.NPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class LoggedOutUserHandler implements Listener {

	private final NPlayer               plugin;
	private final Map<String, Location> loggedOutPlayers;

	public LoggedOutUserHandler(NPlayer plugin) {
		this.plugin = plugin;
		this.loggedOutPlayers = new HashMap<>();

		Bukkit.getScheduler().runTaskTimer(plugin, new BukkitRunnable() {

			@Override
			public void run() {
				poisonLoggedOutPlayers();
			}
		}, 20 * 5, 20 * 5);
	}

	public void notifyConnect(String userName) {
		Player player = Bukkit.getPlayerExact(userName);
		loggedOutPlayers.put(userName, player == null ? null : player.getLocation());
		lockPlayer(userName);
	}

	public void notifyDisconnect(String userName) {
		unlockPlayer(userName);
		loggedOutPlayers.remove(userName);
	}

	public void notifyLogin(User user) {
		loggedOutPlayers.remove(user.getUserName());
		unlockPlayer(user.getUserName());
	}

	public void notifyLogout(User user) {
		Player player = Bukkit.getPlayerExact(user.getUserName());
		loggedOutPlayers.put(user.getUserName(), player == null ? null : player.getLocation());
		lockPlayer(user.getUserName());
	}

	public void poisonLoggedOutPlayers() {
		for (String userName : loggedOutPlayers.keySet()) {
			lockPlayer(userName);
		}
	}

	public void lockPlayer(String userName) {
		Player player = plugin.getServer().getPlayerExact(userName);
		if (player != null) {
			player.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(1337, 42)); // TODO Think about values
		}
	}

	public void unlockPlayer(String userName) {
		Player player = plugin.getServer().getPlayerExact(userName);
		if (player != null) {
			player.removePotionEffect(PotionEffectType.BLINDNESS);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoined(PlayerJoinedEvent event) {
		final Player player = event.getPlayer();
		final String userName = player.getName();
		notifyConnect(userName);
		final User user = plugin.getUserDb().get(userName);
		if (user == null) {
			// Unknown, should /register
			plugin.sendMessage(player, MessageId.player_pleaseRegister);
		} else if (user.getLastIp().equals(event.getPlayer().getAddress().getAddress().getHostAddress()) && !user.hasAutoLogout()) {
			// Auto-login
			user.setLoggedIn(true);
			notifyLogin(user);
			plugin.sendMessage(player, MessageId.player_autoLogged);
		} else {
			// Should /login
			plugin.sendMessage(player, MessageId.player_pleaseLogin);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		String userName = event.getPlayer().getName();
		User user = plugin.getUserDb().get(userName);
		if (user != null && user.hasAutoLogout()) {
			user.setLoggedIn(false);
			notifyLogout(user);
		}
		notifyDisconnect(userName);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		final Location from = event.getFrom();
		final Location to = event.getTo();
		if (loggedOutPlayers.containsKey(event.getPlayer().getName()) && isGridMove(from, to)) {
			final double x = from.getBlockX() + 0.5;
			final double y = from.getBlockY();
			final double z = from.getBlockZ() + 0.5;
			final float yaw = to.getYaw();
			final float pitch = to.getPitch();
			event.getPlayer().teleport(new Location(from.getWorld(), x, y, z, yaw, pitch));
			lockPlayer(event.getPlayer().getName());
		}
	}

	private boolean isGridMove(Location from, Location to) {
		return from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		synchronized (loggedOutPlayers) {
			if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName()) &&
		    !event.getMessage().startsWith("/login") &&
		    !event.getMessage().startsWith("/register")) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInventoryClick(InventoryClickEvent event) {
		if (loggedOutPlayers.containsKey(event.getWhoClicked().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInventoryOpen(InventoryOpenEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerExpChange(PlayerExpChangeEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setAmount(0);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerEditBook(PlayerEditBookEvent event) {
		if (loggedOutPlayers.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerDamage(EntityDamageEvent event) {
		if (event.getEntityType() == EntityType.PLAYER && loggedOutPlayers.containsKey(((Player) event.getEntity()).getName())) {
			event.setCancelled(true);
		}
	}

}

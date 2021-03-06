/***************************************************************************
 * Project file:    NPlugins - NPlayer - PunishmentDb.java                 *
 * Full Class name: fr.ribesg.bukkit.nplayer.punishment.PunishmentDb       *
 *                                                                         *
 *                Copyright (c) 2013 Ribesg - www.ribesg.fr                *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.nplayer.punishment;
import fr.ribesg.bukkit.nplayer.NPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PunishmentDb {

	private final NPlayer plugin;

	private final Map<String, Set<Punishment>> permPunishments;
	private final Map<String, Set<Punishment>> tempPunishments;

	private final Map<String, String> leaveMessages;

	public PunishmentDb(NPlayer instance) {
		this.plugin = instance;
		this.permPunishments = new HashMap<>();
		this.tempPunishments = new HashMap<>();
		this.leaveMessages = new HashMap<>();
	}

	// ##################### //
	// ## Config handling ## //
	// ##################### //

	public void saveConfig() throws IOException {
		saveConfig(Paths.get(plugin.getDataFolder().getPath(), "punishmentDB.yml"));
	}

	private void saveConfig(Path filePath) throws IOException {
		if (!Files.exists(filePath.getParent())) {
			Files.createDirectories(filePath.getParent());
		}
		if (!Files.exists(filePath)) {
			Files.createFile(filePath);
		}
		final YamlConfiguration config = new YamlConfiguration();

		final List<Punishment> punishments = getAllPunishmentsFromMaps(getPermPunishments(), getTempPunishments());

		for (final Punishment p : punishments) {
			final String key = p.getPunished().replaceAll(".", "-") + p.getType().toString(); // Shjould be unique
			final ConfigurationSection pSection = config.createSection(key);
			pSection.set("punished", p.getPunished());
			pSection.set("type", p.getType().toString());
			pSection.set("endDate", p.getEndDate());
			pSection.set("reason", p.getReason());
			if (p.getType() == PunishmentType.JAIL) {
				pSection.set("jailPointName", ((Jail) p).getJailPointName());
			}
		}

		config.save(filePath.toFile());
	}

	public void loadConfig() throws IOException, InvalidConfigurationException {
		loadConfig(Paths.get(plugin.getDataFolder().getPath(), "punishmentDB.yml"));
	}

	private void loadConfig(Path filePath) throws IOException, InvalidConfigurationException {
		if (!Files.exists(filePath)) {
			return; // Nothing to load
		}
		final YamlConfiguration config = new YamlConfiguration();
		config.load(filePath.toFile());

		for (final String key : config.getKeys(false)) {
			if (config.isConfigurationSection(key)) {
				final ConfigurationSection pSection = config.getConfigurationSection(key);
				Punishment p = null;
				final String punished = pSection.getString("punished");
				final PunishmentType type = PunishmentType.valueOf(pSection.getString("type"));
				final long endDate = Long.parseLong(pSection.getString("endDate"));
				final String reason = pSection.getString("reason");
				switch (type) {
					case BAN:
						p = new Ban(punished, reason, endDate);
						break;
					case IPBAN:
						p = new IpBan(punished, reason, endDate);
						break;
					case MUTE:
						p = new Mute(punished, reason, endDate);
						break;
					case JAIL:
						final String jailPointName = pSection.getString("jailPointName");
						p = new Jail(punished, reason, jailPointName, endDate);
						break;
				}

				Map<String, Set<Punishment>> relatedMap;
				if (p.isPermanent()) {
					relatedMap = getPermPunishments();
				} else {
					relatedMap = getTempPunishments();
				}

				Set<Punishment> set = relatedMap.get(p.getPunished());
				if (set == null) {
					set = new HashSet<>();
				}
				set.add(p);
				relatedMap.put(p.getPunished(), set);
			}
		}

	}

	// ############# //
	// ## Getters ## //
	// ############# //

	public Map<String, Set<Punishment>> getPermPunishments() {
		return permPunishments;
	}

	@SafeVarargs
	private static List<Punishment> getAllPunishmentsFromMaps(final Map<String, Set<Punishment>>... maps) {
		final List<Punishment> result = new ArrayList<>();
		for (final Map<String, Set<Punishment>> map : maps) {
			for (Set<Punishment> set : map.values()) {
				result.addAll(set);
			}
		}
		return result;
	}

	public Map<String, Set<Punishment>> getTempPunishments() {
		return tempPunishments;
	}

	public Map<String, String> getLeaveMessages() {
		return leaveMessages;
	}

	// ################## //
	// ## Nickname Ban ## //
	// ################## //

	public boolean tempBanNick(final String playerName, final long duration, final String reason) {
		if (get(playerName, PunishmentType.BAN) != null) {
			return false;
		}
		final Punishment newBan = new Ban(playerName, reason, System.currentTimeMillis() + duration * 1000);
		Set<Punishment> playerPunishments = getTempPunishments().get(playerName);
		if (playerPunishments == null) {
			playerPunishments = new HashSet<>();
		}
		playerPunishments.add(newBan);
		getTempPunishments().put(playerName, playerPunishments);
		return true;
	}

	public boolean permBanNick(final String playerName, final String reason) {
		if (get(playerName, PunishmentType.BAN) != null) {
			return false;
		}
		final Punishment newBan = new Ban(playerName, reason);
		Set<Punishment> playerPunishments = getPermPunishments().get(playerName);
		if (playerPunishments == null) {
			playerPunishments = new HashSet<>();
		}
		playerPunishments.add(newBan);
		getPermPunishments().put(playerName, playerPunishments);
		return true;
	}

	public boolean unbanNick(final String playerName) {
		final Punishment p = get(playerName, PunishmentType.BAN);
		if (p == null) {
			return false;
		}
		remove(p);
		return true;
	}

	public boolean isNickBanned(final String playerName) {
		return get(playerName, PunishmentType.BAN) != null;
	}

	// ############ //
	// ## IP Ban ## //
	// ############ //

	public boolean tempBanIp(final String ip, final long duration, final String reason) {
		if (get(ip, PunishmentType.IPBAN) != null) {
			return false;
		}
		final Punishment newBan = new IpBan(ip, reason, System.currentTimeMillis() + duration * 1000);
		Set<Punishment> ipPunishments = getTempPunishments().get(ip);
		if (ipPunishments == null) {
			ipPunishments = new HashSet<>();
		}
		ipPunishments.add(newBan);
		getTempPunishments().put(ip, ipPunishments);
		return true;
	}

	public boolean permBanIp(final String ip, final String reason) {
		if (get(ip, PunishmentType.IPBAN) != null) {
			return false;
		}
		final Punishment newBan = new IpBan(ip, reason);
		Set<Punishment> ipPunishments = getPermPunishments().get(ip);
		if (ipPunishments == null) {
			ipPunishments = new HashSet<>();
		}
		ipPunishments.add(newBan);
		getPermPunishments().put(ip, ipPunishments);
		return true;
	}

	public boolean unbanIp(final String ip) {
		final Punishment p = get(ip, PunishmentType.IPBAN);
		if (p == null) {
			return false;
		}
		remove(p);
		return true;
	}

	public boolean isIpBanned(final String ip) {
		return get(ip, PunishmentType.IPBAN) != null;
	}

	// ################### //
	// ## Nickname Mute ## //
	// ################### //

	public boolean tempMuteNick(final String playerName, final long duration, final String reason) {
		if (get(playerName, PunishmentType.MUTE) != null) {
			return false;
		}
		final Punishment newMute = new Mute(playerName, reason, System.currentTimeMillis() + duration * 1000);
		Set<Punishment> playerPunishments = getTempPunishments().get(playerName);
		if (playerPunishments == null) {
			playerPunishments = new HashSet<>();
		}
		playerPunishments.add(newMute);
		getTempPunishments().put(playerName, playerPunishments);
		return true;
	}

	public boolean permMuteNick(final String playerName, final String reason) {
		if (get(playerName, PunishmentType.MUTE) != null) {
			return false;
		}
		final Punishment newMute = new Mute(playerName, reason);
		Set<Punishment> playerPunishments = getPermPunishments().get(playerName);
		if (playerPunishments == null) {
			playerPunishments = new HashSet<>();
		}
		playerPunishments.add(newMute);
		getPermPunishments().put(playerName, playerPunishments);
		return true;
	}

	public boolean unmuteNick(final String playerName) {
		final Punishment p = get(playerName, PunishmentType.MUTE);
		if (p == null) {
			return false;
		}
		remove(p);
		return true;
	}

	public boolean isNickMuted(final String playerName) {
		return get(playerName, PunishmentType.MUTE) != null;
	}

	// ######################## //
	// ## Convenient methods ## //
	// ######################## //

	public Punishment get(final String key, final PunishmentType type) {
		final Set<Punishment> temporary = getTempPunishments().get(key);
		if (temporary != null) {
			final Iterator<Punishment> it = temporary.iterator();
			while (it.hasNext()) {
				final Punishment p = it.next();
				if (!p.isStillActive()) {
					it.remove();
				} else if (p.getType() == type) {
					return p;
				}
			}
			if (temporary.isEmpty()) {
				getTempPunishments().remove(key);
			}
		}
		final Set<Punishment> permanent = getPermPunishments().get(key);
		if (permanent != null) {
			for (final Punishment p : permanent) {
				if (p.getType() == type) {
					return p;
				}
			}
		}
		return null;
	}

	private Punishment remove(final Punishment toBeRemoved) {
		final String key = toBeRemoved.getPunished();
		if (toBeRemoved.isPermanent()) {
			final Set<Punishment> permanent = getPermPunishments().get(key);
			if (permanent != null) {
				if (permanent.remove(toBeRemoved)) {
					if (permanent.isEmpty()) {
						getPermPunishments().remove(key);
					}
					return toBeRemoved;
				} else {
					return null;
				}
			}
		} else {
			final Set<Punishment> temporary = getTempPunishments().get(key);
			if (temporary != null) {
				if (temporary.remove(toBeRemoved)) {
					if (temporary.isEmpty()) {
						getTempPunishments().remove(key);
					}
					return toBeRemoved;
				} else {
					return null;
				}
			}
		}
		return null;
	}
}

/***************************************************************************
 * Project file:    NPlugins - NPlayer - IpBan.java                        *
 * Full Class name: fr.ribesg.bukkit.nplayer.punishment.IpBan              *
 *                                                                         *
 *                Copyright (c) 2013 Ribesg - www.ribesg.fr                *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.nplayer.punishment;
public class IpBan extends Punishment {

	public IpBan(String banned, String reason) {
		super(banned, PunishmentType.IPBAN, reason);
	}

	public IpBan(String banned, String reason, long endDate) {
		super(banned, PunishmentType.IPBAN, reason, endDate);
	}
}

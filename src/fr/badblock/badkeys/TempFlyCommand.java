package fr.badblock.badkeys;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempFlyCommand implements CommandExecutor
{

	public static Map<UUID, Long> maps = new HashMap<>();

	public TempFlyCommand()
	{
		Bukkit.getScheduler().runTaskTimer(BadKeys.instance, new Runnable()
		{
			@Override
			public void run()
			{
				Iterator<Entry<UUID, Long>> iterator = maps.entrySet().iterator();
				while (iterator.hasNext())
				{
					Entry<UUID, Long> entry = iterator.next();
					if (entry.getValue() < System.currentTimeMillis())
					{
						Player player = Bukkit.getPlayer(entry.getKey());
						
						if (player == null)
						{
							continue;
						}
						
						player.setFlying(false);
						player.setAllowFlight(false);
						player.sendMessage("§cDésactivation du fly.");
						iterator.remove();
					}
				}
			}
		}, 20, 20);
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (!arg0.hasPermission("tempfly"))
		{
			arg0.sendMessage("§cVous n'avez pas la permission d'exécuter cette commande.");
			return false;
		}

		if (arg3.length < 1)
		{
			arg0.sendMessage("§eUtilisation: /tempfly <pseudo>");
			return true;
		}

		String playerName = arg3[0];

		Player player = Bukkit.getPlayer(playerName);

		if (player == null)
		{
			return false;
		}

		maps.put(player.getUniqueId(), System.currentTimeMillis() + (60 * 30 * 1000L));
		player.setAllowFlight(true);
		player.setFlying(true);
		player.sendMessage("§aActivation du fly pendant 30 minutes.");

		return false;
	}

}

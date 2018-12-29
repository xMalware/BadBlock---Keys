package fr.badblock.badkeys;

import java.sql.ResultSet;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import fr.badblock.badkeys.Request.RequestType;
import net.md_5.bungee.api.ChatColor;

public class BadKeyCommand implements CommandExecutor
{

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (!arg0.hasPermission("badkeys.give"))
		{
			arg0.sendMessage("§cVous n'avez pas la permission d'exécuter cette commande.");
			return false;
		}

		if (arg3.length < 1)
		{
			arg0.sendMessage("§eUtilisation: /badkey <pseudo> [id]");
			return true;
		}

		String playerName = arg3[0];

		if (arg3.length < 2)
		{
			BadblockDatabase.getInstance().addRequest(new Request("SELECT * FROM `keys` WHERE player = '" +
					BadblockDatabase.getInstance().mysql_real_escape_string(playerName) + "' && server = '" + BadKeys.instance.server + "'", RequestType.GETTER)
			{
				@Override
				public void done(ResultSet resultSet)
				{
					try
					{
						arg0.sendMessage("§eClés non utilisées de " + playerName + " :");
						boolean f = false;
						while (resultSet.next())
						{
							f = true;
							int keyId = resultSet.getInt("key");
							String name = BadKeys.instance.names.containsKey(keyId) ? ChatColor.translateAlternateColorCodes('&', BadKeys.instance.names.get(keyId)) : "Inconnu";
							arg0.sendMessage("§aClé " + keyId +" (" + name+ "§a)");
						}
						if (!f)
						{
							arg0.sendMessage("§cAucune clé non utilisée.");
						}
					}
					catch (Exception error)
					{
						arg0.sendMessage("§cUne erreur est survenue.");
					}
				}
			});
			return true;
		}

		String rawId = arg3[1];

		try
		{
			int id = Integer.parseInt(rawId);
			BadblockDatabase.getInstance().addRequest(new Request("INSERT INTO `keys`(player, server, `key`) VALUES('" +
					BadblockDatabase.getInstance().mysql_real_escape_string(playerName) + "', '" + BadKeys.instance.server + "', '" + id + "')", RequestType.SETTER));
			System.out.println("[BadKeys] Added key for " + playerName + " (key id " + id + ") -> added by " + arg0.getName());
			arg0.sendMessage("§aClé '" + id + "' donnée à " + playerName + ".");
			return true;
		}
		catch (Exception error)
		{
			arg0.sendMessage("§cL'ID doit être un nombre.");
		}

		return false;
	}

}

package fr.badblock.badkeys;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.badblock.badkeys.Request.RequestType;

public class ClickListener implements Listener
{

	public Map<String, Long>	wait = new HashMap<>();
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();

		Action action = event.getAction();

		if (!action.equals(Action.LEFT_CLICK_BLOCK) && !action.equals(Action.RIGHT_CLICK_BLOCK))
		{
			return;
		}

		Block block = event.getClickedBlock();

		if (block == null)
		{
			return;
		}

		Location location = block.getLocation();

		for (Location loc : BadKeys.instance.locations)
		{
			if (!loc.equals(location))
			{
				continue;
			}

			event.setCancelled(true);

			openInventory(player);
			break;
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event)
	{
		HumanEntity humanEntity = event.getWhoClicked();

		if (humanEntity == null)
		{
			return;
		}

		Player player = (Player) humanEntity;

		Inventory inventory = event.getInventory();

		if (inventory == null)
		{
			return;
		}

		if (inventory.getName() == null)
		{
			return;
		}

		if (!inventory.getName().equalsIgnoreCase("§bUtiliser une clé"))
		{
			return;
		}

		event.setCancelled(true);

		ItemStack itemStack = event.getCurrentItem();

		if (itemStack == null)
		{
			return;
		}

		if (!BadKeys.instance.materials.containsValue(itemStack.getType()))
		{
			return;
		}

		Material material = itemStack.getType();

		for (Entry<Integer, Material> entry : BadKeys.instance.materials.entrySet())
		{
			if (!entry.getValue().equals(material))
			{
				continue;
			}

			int key = entry.getKey();

			BadblockDatabase.getInstance().addRequest(new Request("SELECT * FROM `keys` WHERE player = '" + 
					humanEntity.getName() + "' && server = '" + BadKeys.instance.server + "' && `key` = '" + key + "'",
					RequestType.GETTER)
			{
				@Override
				public void done(ResultSet resultSet)
				{
					try
					{
						if (!resultSet.next())
						{
							player.sendMessage("§7[§cClés§7] §cClé inexistante.");
							player.closeInventory();
							return;
						}
					}
					catch (Exception error)
					{
						error.printStackTrace();
						return;
					}
					
					if (!wait.containsKey(player.getName().toLowerCase()) && (wait.containsKey(player.getName().toLowerCase())
							&& wait.get(player.getName().toLowerCase()) > System.currentTimeMillis()))
					{
						player.sendMessage("§7[§cClés§7] §cVeuillez patienter quelques secondes");
						player.sendMessage("§7[§cClés§7] §centre chaque utilisation de clé.");
						return;
					}
					
					wait.put(player.getName().toLowerCase(), System.currentTimeMillis() + 30_000L);

					if (!BadKeys.instance.rewards.containsKey(key))
					{
						player.sendMessage("§7[§cClés§7] §cCode d'erreur (code 3) : " + key);
						return;
					}
					
					int rewards = BadKeys.instance.rewards.get(key);
					
					try
					{
						int id = resultSet.getInt("id");
						BadblockDatabase.getInstance().addRequest(new Request("DELETE FROM `keys` WHERE `id` = '" + id + "'", RequestType.SETTER));
					}
					catch (SQLException exception)
					{
						exception.printStackTrace();
						player.sendMessage("§7[§cClés§7] §cErreur (code 2).");
						return;
					}

					player.sendMessage("§7[§cClés§7] §aVous avez utilisé une clé.");

					try
					{
						for (int l = 1; l <= rewards; l++)
						{
							String dot = "........";
							for (int v = 8; v > 0; v--)
							{
								dot = dot.substring(0, v);
								player.sendMessage("§7[§cClés§7] §e" + l + "e lot" + dot);
								Thread.sleep(150);
							}
							final int og = l;
							BadblockDatabase.getInstance().addRequest(new Request("SELECT * FROM `keyProbabilities` WHERE `key` = '" + key + "'",
									RequestType.GETTER)
							{
								@Override
								public void done(ResultSet resultSet)
								{
									List<ProbaItem> probaItems = new ArrayList<>();
									try
									{
										while (resultSet.next())
										{
											String itemName = resultSet.getString("name");
											int probability = resultSet.getInt("probability");
											String command = resultSet.getString("command");

											ProbaItem item = new ProbaItem(itemName, probability, command);
											probaItems.add(item);
										}
									}
									catch (Exception error)
									{
										player.sendMessage("§cErreur survenue avec les clés (code 1)");
										error.printStackTrace();
									}
									Bukkit.getScheduler().runTask(BadKeys.instance, new Runnable()
									{

										@Override
										public void run()
										{
											int total = probaItems.stream().mapToInt(probaItem -> probaItem.probability).sum();
											int random = new SecureRandom().nextInt(total);
											int i = 0;
											Map<Integer, ProbaItem> map = new HashMap<>();
											for (ProbaItem probaItem : probaItems)
											{
												for (int o = 0; o < probaItem.probability; o++)
												{
													i++;
													map.put(i, probaItem);
												}
											}
											ProbaItem probaItem = map.get(random);
											if (probaItem == null)
											{
												probaItem = map.get(0);
											}
											player.closeInventory();
											player.sendMessage("§7[§cClés§7] §aLot " + og + " donné : §d" + probaItem.itemName);
											Bukkit.dispatchCommand(Bukkit.getConsoleSender(), probaItem.command.replace("%player%", player.getName()));
											String rawName = Integer.toString(key);
											if (BadKeys.instance.names.containsKey(key))
											{
												rawName = BadKeys.instance.names.get(key);
											}
											rawName = ChatColor.translateAlternateColorCodes('&', rawName);
											Bukkit.broadcastMessage("§7[§cClés§7] §a" + player.getName() + " a gagné " + probaItem.itemName + " §aen utilisant une " + rawName + "§a.");
											Bukkit.broadcastMessage("§7[§cClés§7] §eClés disponibles à prix cassé sur la boutique !");
											Bukkit.broadcastMessage("§7[§cClés§7] §eBoutique : §b§nhttps://boutique.badblock.fr/");
										}

									});
								}
							});
							Thread.sleep(1000);
						}


					}
					catch (Exception error)
					{
						error.printStackTrace();
					}

				}
			});
		}
	}

	public void openInventory(Player player)
	{
		BadblockDatabase.getInstance().addRequest(new Request("SELECT `key` FROM `keys` WHERE player = '" + player.getName()
		+ "' && server = '" + BadKeys.instance.server + "'", RequestType.GETTER)
		{
			@Override
			public void done(ResultSet resultSet)
			{
				List<Integer> ids = new ArrayList<>();
				try
				{
					while (resultSet.next())
					{
						int key = resultSet.getInt("key");
						ids.add(key);
					}
				}
				catch (Exception error)
				{
					error.printStackTrace();
				}

				if (ids.isEmpty())
				{
					player.sendMessage("§7[§cClés§7] §cVous n'avez aucune clé non utilisée.");
					player.sendMessage("§eObtenez dès maintenant des clés sur la boutique.");
					player.sendMessage("§eBoutique : §b§nhttps://boutique.badblock.fr/");
					return;
				}

				Map<Integer, Integer> nb = new HashMap<>();

				for (int i : ids)
				{
					int b = !nb.containsKey(i) ? 1 : nb.get(i) + 1;
					nb.put(i, b);
				}

				Inventory inventory = Bukkit.createInventory(null, 9, "§bUtiliser une clé");

				for (Entry<Integer, Integer> entry : nb.entrySet())
				{
					String rawName = Integer.toString(entry.getKey());
					if (BadKeys.instance.names.containsKey(entry.getKey()))
					{
						rawName = BadKeys.instance.names.get(entry.getKey());
					}
					Material material = null;
					if (!BadKeys.instance.materials.containsKey(entry.getKey()))
					{
						continue;
					}
					material = BadKeys.instance.materials.get(entry.getKey());
					ItemStack itemStack = new ItemStack(material, entry.getValue());
					ItemMeta itemMeta = itemStack.getItemMeta();
					itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rawName));
					itemMeta.setLore(Arrays.asList("", "§bClic gauche/droit : §7Utiliser cette clé", "",
							"§cN'oubliez pas d'avoir assez d'espace", "§cdans votre inventaire."));
					itemStack.setItemMeta(itemMeta);
					inventory.addItem(itemStack);
				}

				Bukkit.getScheduler().runTask(BadKeys.instance, new Runnable()
				{
					@Override
					public void run()
					{
						player.openInventory(inventory);
					}
				});
			}
		});
	}

}

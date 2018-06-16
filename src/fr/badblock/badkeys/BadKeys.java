package fr.badblock.badkeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class BadKeys extends JavaPlugin
{

	public static BadKeys			instance;

	public String					server;
	public List<Location>			locations;
	public Map<Integer, String>		names;
	public Map<Integer, Integer>	rewards;
	public Map<Integer, Material>	materials;

	@Override
	public void onEnable()
	{
		instance = this;

		// Create a list
		locations = new ArrayList<>();
		// Create a map
		names = new HashMap<>();
		// Create a map
		materials = new HashMap<>();
		// Create a map
		rewards = new HashMap<>();

		// Reload config
		this.reloadConfig();

		ConfigurationSection loc = getConfig().getConfigurationSection("locations");

		for (String string : loc.getKeys(false))
		{
			ConfigurationSection section = loc.getConfigurationSection(string);
			String rawWorld = section.getString("world");
			int x = section.getInt("x");
			int y = section.getInt("y");
			int z = section.getInt("z");
			World world = Bukkit.getWorld(rawWorld);
			Location location = new Location(world, x, y, z);
			locations.add(location);
		}

		ConfigurationSection keys = getConfig().getConfigurationSection("keys");

		for (String string : keys.getKeys(false))
		{
			ConfigurationSection section = keys.getConfigurationSection(string);
			String name = section.getString("name");
			int id = -1;
			try
			{
				id = Integer.parseInt(string);
			}
			catch (Exception error)
			{
				error.printStackTrace();
			}
			int item = section.getInt("item");
			int reward = section.getInt("rewards");
			@SuppressWarnings("deprecation")
			Material material = Material.getMaterial(item);
			names.put(id, name);
			materials.put(id, material);
			rewards.put(id, reward);
		}

		ConfigurationSection database = getConfig().getConfigurationSection("database");
		String hostname = database.getString("hostname");
		int port = database.getInt("port");
		String username = database.getString("username");
		String password = database.getString("password");
		String db = database.getString("database");
		// Connect
		BadblockDatabase.getInstance().connect(hostname, port, username, password, db);

		server = getConfig().getString("server");
		
		// Register the event
		getServer().getPluginManager().registerEvents(new ClickListener(), this);
	}

}

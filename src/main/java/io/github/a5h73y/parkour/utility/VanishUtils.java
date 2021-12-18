package io.github.a5h73y.parkour.utility;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.kitteh.vanish.VanishPerms;
import org.kitteh.vanish.VanishPlugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import de.myzelyam.api.vanish.VanishAPI;

public class VanishUtils {
	
	
	public static boolean isVanished(@NotNull Player player) {
		Objects.requireNonNull(player, "player cannot be null");
		if (player.getMetadata("vanished").stream().anyMatch(MetadataValue::asBoolean)) return true;
		
		if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
			Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
			User user = ess.getUser(player);
			if (user.isVanished()) return true;
		}
		
		return false;
	}
	
	
	public static boolean canSee(@NotNull Player player, @NotNull Player target) {
		Objects.requireNonNull(player, "player cannot be null");
		Objects.requireNonNull(target, "target cannot be null");
		if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
			Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
			User user = ess.getUser(target);
			if (user.isHiddenFrom(player)) return false;
		}
		if (Bukkit.getPluginManager().getPlugin("VanishNoPacket") != null) {
			VanishPlugin vnp = (VanishPlugin) Bukkit.getPluginManager().getPlugin("VanishNoPacket");
			if (vnp.getManager().isVanished(target) && !VanishPerms.canSeeAll(player)) return false;
		}
		if (Bukkit.getPluginManager().getPlugin("SuperVanish") != null || Bukkit.getPluginManager().getPlugin("PremiumVanish") != null) {
			if (!VanishAPI.canSee(player, target)) return false;
		}
		return true;
	}
	

}

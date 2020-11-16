package io.github.a5h73y.parkour.other;

import io.github.a5h73y.parkour.Parkour;
import io.github.a5h73y.parkour.enums.ConfigType;
import io.github.a5h73y.parkour.enums.Permission;
import io.github.a5h73y.parkour.plugin.EconomyApi;
import io.github.a5h73y.parkour.type.course.Course;
import io.github.a5h73y.parkour.type.course.CourseInfo;
import io.github.a5h73y.parkour.type.kit.ParkourKitInfo;
import io.github.a5h73y.parkour.type.lobby.LobbyInfo;
import io.github.a5h73y.parkour.type.player.PlayerInfo;
import io.github.a5h73y.parkour.utility.PermissionUtils;
import io.github.a5h73y.parkour.utility.StringUtils;
import io.github.a5h73y.parkour.utility.TranslationUtils;
import io.github.a5h73y.parkour.utility.ValidationUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Validation {

    /**
     * Validate course creation
     *
     * @param courseName
     * @param player
     * @return
     */
    public static boolean courseCreation(String courseName, Player player) {
        if (courseName.length() > 15) {
            player.sendMessage(Parkour.getPrefix() + "Course name is too long!");
            return false;

        } else if (courseName.contains(".")) {
            player.sendMessage(Parkour.getPrefix() + "Course name can not contain '.'");
            return false;

        } else if (ValidationUtils.isInteger(courseName)) {
            player.sendMessage(Parkour.getPrefix() + "Course name can not only be numeric");
            return false;

        } else if (Parkour.getInstance().getCourseManager().doesCourseExists(courseName)) {
            TranslationUtils.sendTranslation("Error.Exist", player);
            return false;
        }

        return true;
    }

    /**
     * Validate player join course
     *
     * @param player
     * @param course
     * @return
     */
    public static boolean courseJoining(Player player, Course course) {
        /* World doesn't exist */
        if (course.getCheckpoints().isEmpty()) {
            TranslationUtils.sendTranslation("Error.UnknownWorld", player);
            return false;
        }

        /* Player in wrong world */
        if (Parkour.getDefaultConfig().isJoinEnforceWorld()
                && !player.getLocation().getWorld().getName().equals(course.getCheckpoints().get(0).getWorld())) {
            TranslationUtils.sendTranslation("Error.WrongWorld", player);
            return false;
        }

        /* Players level isn't high enough */
        int minimumLevel = CourseInfo.getMinimumParkourLevel(course.getName());

        if (minimumLevel > 0 && !PermissionUtils.hasPermission(player, Permission.ADMIN_LEVEL_BYPASS, false)
                && !PermissionUtils.hasSpecificPermission(player, Permission.PARKOUR_LEVEL, String.valueOf(minimumLevel), false)) {
            int currentLevel = PlayerInfo.getParkourLevel(player);

            if (currentLevel < minimumLevel) {
                TranslationUtils.sendValueTranslation("Error.RequiredLvl", String.valueOf(minimumLevel), player);
                return false;
            }
        }

        /* Permission system */
        if (Parkour.getDefaultConfig().getBoolean("OnJoin.PerCoursePermission")
                && !PermissionUtils.hasSpecificPermission(player, Permission.PARKOUR_COURSE, course.getName(), true)) {
            return false;
        }

        /* Course isn't ready */
        if (!CourseInfo.getReadyStatus(course.getName())) {
            if (Parkour.getDefaultConfig().getBoolean("OnJoin.EnforceReady")) {
                if (!PermissionUtils.hasPermissionOrCourseOwnership(player, Permission.ADMIN_READY_BYPASS, course.getName())) {
                    TranslationUtils.sendTranslation("Error.NotReady", player);
                    return false;
                }
            } else {
                TranslationUtils.sendTranslation("Error.NotReadyWarning", player);
            }
        }

        /* Check if player has enough currency to join */
        if (!Parkour.getInstance().getEconomyApi().validateCourseJoin(player, course.getName())) {
            return false;
        }

        /* Check if the player can leave the course for another */
        if (Parkour.getInstance().getPlayerManager().isPlaying(player)) {
            if (Parkour.getDefaultConfig().getBoolean("OnCourse.PreventJoiningDifferentCourse")) {
                TranslationUtils.sendTranslation("Error.JoiningAnotherCourse", player);
                return false;
            }
            if (Parkour.getDefaultConfig().isCourseEnforceWorld()
                    && !player.getLocation().getWorld().getName().equals(course.getCheckpoints().get(0).getWorld())) {
                TranslationUtils.sendTranslation("Error.WrongWorld", player);
                return false;
            }
        }

        /* Check if player limit exceeded */
        if (CourseInfo.hasPlayerLimit(course.getName())) {
            if (Parkour.getInstance().getPlayerManager().getNumberOfPlayersOnCourse(course.getName()) >= CourseInfo.getPlayerLimit(course.getName())) {
                TranslationUtils.sendTranslation("Error.LimitExceeded", player);
                return false;
            }
        }

        return true;
    }

    /**
     * Validate player joining course, without sending messages
     *
     * @param player
     * @param courseName
     * @return
     */
    public static boolean courseJoiningNoMessages(Player player, String courseName) {
        /* Player in wrong world */
        if (Parkour.getDefaultConfig().isJoinEnforceWorld()) {
            if (!player.getLocation().getWorld().getName().equals(CourseInfo.getWorld(courseName))) {
                return false;
            }
        }

        /* Players level isn't high enough */
        int minimumLevel = CourseInfo.getMinimumParkourLevel(courseName);

        if (minimumLevel > 0) {
            if (!PermissionUtils.hasPermission(player, Permission.ADMIN_LEVEL_BYPASS, false)
                    && !PermissionUtils.hasSpecificPermission(player, Permission.PARKOUR_LEVEL, String.valueOf(minimumLevel), false)) {
                int currentLevel = PlayerInfo.getParkourLevel(player);

                if (currentLevel < minimumLevel) {
                    return false;
                }
            }
        }

        /* Course isn't ready */
        if (!CourseInfo.getReadyStatus(courseName)
                && Parkour.getDefaultConfig().getBoolean("OnJoin.EnforceReady")
                && !PermissionUtils.hasPermissionOrCourseOwnership(player, Permission.ADMIN_READY_BYPASS, courseName)) {
            return false;
        }

        /* Check if player has enough currency to join */
        if (Parkour.getInstance().getEconomyApi().isEnabled()) {
            int joinFee = CourseInfo.getEconomyJoiningFee(courseName);
            return joinFee <= 0 || Parkour.getInstance().getEconomyApi().hasAmount(player, joinFee);
        }

        return true;
    }

    /**
     * Validate challenging a player
     *
     * @param args
     * @param player
     * @return
     */
    public static boolean challengePlayer(Player player, String[] args) {
        String courseName = args[1].toLowerCase();
        Player targetPlayer = Bukkit.getPlayer(args[2]);

        if (targetPlayer == null) {
            player.sendMessage(Parkour.getPrefix() + "This player is not online!");
            return false;
        }
        if (!Parkour.getInstance().getCourseManager().doesCourseExists(courseName)) {
            TranslationUtils.sendValueTranslation("Error.NoExist", courseName, player);
            return false;
        }
        if (Parkour.getInstance().getPlayerManager().isPlaying(player)) {
            player.sendMessage(Parkour.getPrefix() + "You are already on a course!");
            return false;
        }
        if (Parkour.getInstance().getPlayerManager().isPlaying(targetPlayer)) {
            player.sendMessage(Parkour.getPrefix() + "This player is already on a course!");
            return false;
        }
        if (player.getName().equalsIgnoreCase(targetPlayer.getName())) {
            player.sendMessage(Parkour.getPrefix() + "You can't challenge yourself!");
            return false;
        }
        if (!courseJoiningNoMessages(player, courseName)) {
            player.sendMessage(Parkour.getPrefix() + "You are not able to join this course!");
            return false;
        }
        if (!courseJoiningNoMessages(targetPlayer, courseName)) {
            player.sendMessage(Parkour.getPrefix() + "They are not able to join this course!");
            return false;
        }
        if (args.length == 4) {
            String wagerAmount = args[3];
            EconomyApi economyApi = Parkour.getInstance().getEconomyApi();

            if (!economyApi.isEnabled()) {
                player.sendMessage(Parkour.getPrefix() + "Economy is disabled, no wager will be made.");

            } else if (!ValidationUtils.isPositiveDouble(wagerAmount)) {
                player.sendMessage(Parkour.getPrefix() + "Wager must be a positive number.");
                return false;

            } else if (!economyApi.hasAmount(player, Double.parseDouble(wagerAmount))) {
                player.sendMessage(Parkour.getPrefix() + "You do not have enough funds for this wager.");
                return false;

            } else if (!economyApi.hasAmount(targetPlayer, Double.parseDouble(wagerAmount))) {
                player.sendMessage(Parkour.getPrefix() + "They do not have enough funds for this wager.");
                return false;
            }
        }

        return true;
    }

    /**
     * Validate joining a lobby
     *
     * @param player
     * @return boolean
     */
    public static boolean isDefaultLobbySet(Player player) {
        boolean lobbySet = true;
        if (!LobbyInfo.doesLobbyExist()) {
            if (PermissionUtils.hasPermission(player, Permission.ADMIN_ALL)) {
                player.sendMessage(Parkour.getPrefix() + ChatColor.RED + "Default Lobby has not been set!");
                player.sendMessage(StringUtils.colour("Type &b'/pa setlobby'&f where you want the lobby to be set."));

            } else {
                player.sendMessage(Parkour.getPrefix() + ChatColor.RED + "Default Lobby has not been set! Please tell the Owner!");
            }
            lobbySet = false;
        }
        return lobbySet;
    }

    /**
     * Validate joining a custom lobby
     *
     * @param player
     * @param lobbyName
     * @return
     */
    public static boolean canJoinLobby(Player player, String lobbyName) {
        if (!LobbyInfo.doesLobbyExist(lobbyName)) {
            TranslationUtils.sendValueTranslation("Error.UnknownLobby", lobbyName, player);
            return false;
        }

        if (Parkour.getDefaultConfig().getBoolean("LobbySettings.EnforceWorld")
                && !player.getWorld().getName().equals(LobbyInfo.getLobbyLocation(lobbyName).getWorld().getName())) {
            TranslationUtils.sendTranslation("Error.WrongWorld", player);
            return false;
        }

        int level = LobbyInfo.getRequiredLevel(lobbyName);

        if (level > 0
                && !PermissionUtils.hasPermission(player, Permission.ADMIN_LEVEL_BYPASS, false)
                && PlayerInfo.getParkourLevel(player) < level) {
            TranslationUtils.sendValueTranslation("Error.RequiredLvl", String.valueOf(level), player);
            return false;
        }

        return true;
    }

    /**
     * Validate creating a course checkpoint
     *
     * @param args
     * @param player
     * @return
     */
    public static boolean createCheckpoint(Player player, @Nullable Integer checkpoint) {
        String selected = PlayerInfo.getSelectedCourse(player).toLowerCase();

        if (!Parkour.getInstance().getCourseManager().doesCourseExists(selected)) {
            TranslationUtils.sendValueTranslation("Error.NoExist", selected, player);
            return false;
        }

        int pointcount = CourseInfo.getCheckpointAmount(selected) + 1;

        if (checkpoint != null) {
            if (checkpoint < 1) {
                player.sendMessage(Parkour.getPrefix() + "Checkpoint specified is not valid!");
                return false;
            }
            if (pointcount < checkpoint) {
                player.sendMessage(Parkour.getPrefix() + "This checkpoint does not exist! " + ChatColor.RED + "Creation cancelled.");
                return false;
            }

            pointcount = checkpoint;
        }

        if (pointcount < 1) {
            player.sendMessage(Parkour.getPrefix() + "Invalid checkpoint number.");
            return false;
        }
        return true;
    }

    /**
     * Validate a course before deleting it
     *
     * @param courseName
     * @param sender
     * @return
     */
    public static boolean deleteCourse(String courseName, CommandSender sender) {
        if (!Parkour.getInstance().getCourseManager().doesCourseExists(courseName)) {
            TranslationUtils.sendValueTranslation("Error.NoExist", courseName, sender);
            return false;
        }

        courseName = courseName.toLowerCase();
        List<String> dependentCourses = new ArrayList<>();

        for (String course : CourseInfo.getAllCourseNames()) {
            String linkedCourse = CourseInfo.getLinkedCourse(courseName);

            if (courseName.equals(linkedCourse)) {
                dependentCourses.add(course);
            }
        }

        if (dependentCourses.size() > 0) {
            sender.sendMessage(Parkour.getPrefix() + "This course can not be deleted as there are dependent courses: " + dependentCourses);
            return false;
        }

        return true;
    }

    /**
     * Validate a lobby before deleting it
     *
     * @param lobbyName
     * @param player
     * @return
     */
    public static boolean deleteLobby(CommandSender sender, String lobbyName) {
        if (!LobbyInfo.doesLobbyExist(lobbyName)) {
            TranslationUtils.sendValueTranslation("Error.UnknownLobby", lobbyName, sender);
            return false;
        }

        lobbyName = lobbyName.toLowerCase();
        List<String> dependentCourses = new ArrayList<>();

        for (String course : CourseInfo.getAllCourseNames()) {
            String linkedCourse = CourseInfo.getLinkedLobby(lobbyName);

            if (lobbyName.equals(linkedCourse)) {
                dependentCourses.add(course);
            }
        }

        if (dependentCourses.size() > 0) {
            sender.sendMessage(Parkour.getPrefix() + "This lobby can not be deleted as there are dependent courses: "
                    + dependentCourses);
            return false;
        }

        return true;
    }

    /**
     * Validate a kit before deleting it
     *
     * @param parkourKit
     * @param sender
     * @return
     */
    public static boolean deleteParkourKit(CommandSender sender, String parkourKit) {
        if (!ParkourKitInfo.doesParkourKitExist(parkourKit)) {
            TranslationUtils.sendTranslation("Error.UnknownParkourKit", sender);
            return false;
        }

        parkourKit = parkourKit.toLowerCase();
        List<String> dependentCourses = ParkourKitInfo.getDependentCourses(parkourKit);

        if (dependentCourses.size() > 0) {
            sender.sendMessage(Parkour.getPrefix() + "This ParkourKit can not be deleted as there are dependent courses: " + dependentCourses);
            return false;
        }

        return true;
    }

    /**
     * Validate the autostart before deleting it
     *
     * @param courseName
     * @param coordinates
     * @param player
     * @return
     */
    public static boolean deleteAutoStart(Player player, String courseName, String coordinates) {
        if (Parkour.getInstance().getCourseManager().getAutoStartCourse(player.getLocation()) == null) {
            player.sendMessage(Parkour.getPrefix() + "There is no autostart at this location");
            return false;
        }

        courseName = courseName.toLowerCase();
        if (!Parkour.getConfig(ConfigType.COURSES).getString("CourseInfo.AutoStart." + coordinates).equals(courseName)) {
            player.sendMessage(Parkour.getPrefix() + "This autostart can not be deleted as it is not linked to course: " + courseName);
            return false;
        }

        return true;
    }

    public static boolean deleteCheckpoint(CommandSender sender, String courseName) {
        if (!Parkour.getInstance().getCourseManager().doesCourseExists(courseName)) {
            TranslationUtils.sendValueTranslation("Error.NoExist", courseName, sender);
            return false;
        }

        int checkpoints = CourseInfo.getCheckpointAmount(courseName);
        // if it has no checkpoints
        if (checkpoints <= 0) {
            sender.sendMessage(Parkour.getPrefix() + courseName + " has no checkpoints!");
            return false;
        }
        return true;
    }
}

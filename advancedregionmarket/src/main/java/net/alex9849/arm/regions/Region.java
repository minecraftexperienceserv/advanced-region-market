package net.alex9849.arm.regions;

import net.alex9849.arm.AutoPrice;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.alex9849.arm.Main;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;

public abstract class Region {
    private static List<Region> regionList = new ArrayList<>();

    protected ProtectedRegion region;
    protected String regionworld;
    protected ArrayList<Sign> sellsign;
    protected ArrayList<Location> builtblocks;
    protected double price;
    protected boolean sold;
    protected boolean autoreset;
    protected boolean isHotel;
    protected long lastreset;
    protected static int resetcooldown;
    private static YamlConfiguration regionsconf;
    protected RegionKind regionKind;
    protected static double paybackPercentage;
    protected Location teleportLocation;
    protected boolean isDoBlockReset;

    public Region(ProtectedRegion region, String regionworld, List<Sign> sellsign, double price, Boolean sold, Boolean autoreset,
                  Boolean isHotel, Boolean doBlockReset, RegionKind regionKind, Location teleportLoc, long lastreset, Boolean newreg){
        this.region = region;
        this.sellsign = new ArrayList<Sign>(sellsign);
        this.sold = sold;
        this.price = price;
        this.regionworld = regionworld;
        this.regionKind = regionKind;
        this.autoreset = autoreset;
        this.isDoBlockReset = doBlockReset;
        this.lastreset = lastreset;
        this.builtblocks = new ArrayList<Location>();
        this.isHotel = isHotel;
        this.teleportLocation = teleportLoc;

        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File builtblocksdic = new File(pluginfolder + "/schematics/" + this.regionworld + "/" + region.getId() + "--builtblocks.schematic");
        if(builtblocksdic.exists()){
            try {
                FileReader filereader = new FileReader(builtblocksdic);
                BufferedReader reader = new BufferedReader(filereader);
                String line;
                while ((line = reader.readLine()) != null){
                    String[] lines = line.split(";", 4);
                    int x = Integer.parseInt(lines[1]);
                    int y = Integer.parseInt(lines[2]);
                    int z = Integer.parseInt(lines[3]);
                    Location loc = new Location(Bukkit.getWorld(lines[0]), x, y, z);
                    builtblocks.add(loc);
                }
                reader.close();
                filereader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.builtblocks = new ArrayList<Location>();
        }



        if(newreg){
            YamlConfiguration config = getRegionsConf();

            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".price", price);
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".sold", false);
            if(regionKind == RegionKind.DEFAULT) {
                config.set("Regions." + this.regionworld + "." + this.region.getId() + ".kind", "default");
            } else {
                config.set("Regions." + this.regionworld + "." + this.region.getId() + ".kind", regionKind.getName());
            }
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".regiontype", "sellregion");
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".autoreset", autoreset);
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".lastreset", lastreset);
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".isHotel", isHotel);
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".doBlockReset", doBlockReset);
            List<String> regionsigns = config.getStringList("Regions." + this.regionworld + "." + this.region.getId() + ".signs");
            Location signloc = this.sellsign.get(0).getLocation();
            regionsigns.add(signloc.getWorld().getName() + ";" + signloc.getX() + ";" + signloc.getY() + ";" + signloc.getZ());
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".signs", regionsigns);
            saveRegionsConf(config);
            this.createSchematic();
        }

    }

    public void setTeleportLocation(Location loc) {
        this.teleportLocation = loc;
        String locString = loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ() + ";" + loc.getPitch() + ";" + loc.getYaw();
        YamlConfiguration conf = getRegionsConf();
        conf.set("Regions." + this.regionworld + "." + this.region.getId() + ".teleportLoc", locString);
        saveRegionsConf(conf);
    }

    public void addBuiltBlock(Location loc) {
        this.builtblocks.add(loc);
        try {
            File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
            File builtblocksdic = new File(pluginfolder + "/schematics/" + this.regionworld + "/" + region.getId() + "--builtblocks.schematic");
            if(!builtblocksdic.exists()){
                File builtblocksfolder = new File(pluginfolder + "/schematics/" + this.regionworld);
                builtblocksfolder.mkdirs();
                builtblocksdic.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(builtblocksdic, true);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            loc.getBlock().getType();
            writer.write(loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ());
            writer.newLine();
            writer.close();
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSign(Location loc){
        Sign newsign = (Sign) loc.getBlock().getState();
        sellsign.add(newsign);
        YamlConfiguration config = getRegionsConf();
        List<String> regionsigns = config.getStringList("Regions." + this.regionworld + "." + region.getId() + ".signs");
        regionsigns.add(loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ());
        config.set("Regions." + this.regionworld + "." + region.getId() + ".signs", regionsigns);
        saveRegionsConf(config);
        this.updateSignText(newsign);
        Bukkit.getServer().getWorld(regionworld).save();

    }

    public boolean removeSign(Location loc){
        return this.removeSign(loc, null);
    }

    public boolean removeSign(Location loc, Player destroyer){
        for(int i = 0; i < this.sellsign.size(); i++){
            if(this.sellsign.get(i).getWorld().getName().equalsIgnoreCase(loc.getWorld().getName())){
                if(this.sellsign.get(i).getLocation().distance(loc) == 0){
                    this.sellsign.remove(i);
                    YamlConfiguration config = getRegionsConf();
                    List<String> regionSigns = config.getStringList("Regions." + this.regionworld + "." + this.region.getId() + ".signs");
                    for (int j = 0; j < regionSigns.size(); j++){
                        if (regionSigns.get(j).equals(this.regionworld + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ())){
                            regionSigns.remove(j);
                            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".signs", regionSigns);
                            saveRegionsConf(config);
                            if(destroyer != null) {
                                String message = Messages.SIGN_REMOVED_FROM_REGION.replace("%remaining%", this.getNumberOfSigns() + "");
                                destroyer.sendMessage(Messages.PREFIX + message);
                            }
                            if(regionSigns.size() == 0){
                                for(int x = 0; x < Region.getRegionList().size(); x++) {
                                    if(Region.getRegionList().get(x) == this){
                                        config.set("Regions." + this.regionworld + "." + this.region.getId(), null);
                                        saveRegionsConf(config);
                                        if(destroyer != null) {
                                            destroyer.sendMessage(Messages.PREFIX + Messages.REGION_REMOVED_FROM_ARM);
                                        }
                                        Region.getRegionList().remove(x);
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public void updateSigns() {
        for (int i = 0; i < this.sellsign.size(); i++) {
            Location loc = new Location(this.sellsign.get(i).getLocation().getWorld(), this.sellsign.get(i).getLocation().getBlockX(), this.sellsign.get(i).getLocation().getBlockY(), this.sellsign.get(i).getLocation().getBlockZ());
            if (loc.getBlock().getType() != this.sellsign.get(i).getType()) {
                loc.getBlock().setType(this.sellsign.get(i).getType());
                loc.getBlock().setBlockData(this.sellsign.get(i).getBlockData());
                this.sellsign.set(i, (Sign) loc.getBlock().getState());

                Bukkit.getLogger().log(Level.INFO, loc.getBlock().getBlockData().getAsString());
            }

            this.updateSignText(this.sellsign.get(i));
        }
    }

    public String getRegionworld() {
        return regionworld;
    }

    public double getPrice(){
        return this.price;
    }

    public boolean hasSign(Sign sign){
        for(int i = 0; i < this.sellsign.size(); i++){
            if(this.sellsign.get(i).getWorld().getName().equalsIgnoreCase(sign.getWorld().getName())){
                if(this.sellsign.get(i).getLocation().distance(sign.getLocation()) == 0){
                    return true;
                }
            }
        }
        return false;
    }

    public int getNumberOfSigns(){
        return this.sellsign.size();
    }

    public static double calculatePrice(ProtectedRegion region, String regiontype){

        int maxX = region.getMaximumPoint().getBlockX();
        int minX = region.getMinimumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int maxZ = region.getMaximumPoint().getBlockZ();
        int minZ = region.getMinimumPoint().getBlockZ();
        double price = 0;
        double priceperblock = 0;

        for(int i = 0; i < AutoPrice.getAutoPrices().size(); i++){
            if(AutoPrice.getAutoPrices().get(i).equals(regiontype)){
                priceperblock = AutoPrice.getAutoPrices().get(i).getPricePerSquareMeter();
            }
        }

        for(int x = minX; x <= maxX; x++){
            for(int z = minZ; z <= maxZ; z++) {
                if(region.contains(x, minY, z)){
                    price = price + priceperblock;
                }
            }
        }
        if(price == 0) {
            return Double.parseDouble(regiontype);
        } else {
            return price;
        }
    }

    public boolean setKind(String kind){
        RegionKind regionkind = RegionKind.getRegionKind(kind);

        if(regionkind == null) {
            return false;

        } else if(regionkind == RegionKind.DEFAULT) {
            YamlConfiguration config = getRegionsConf();
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".kind", "default");
            saveRegionsConf(config);
            this.regionKind = RegionKind.DEFAULT;
            return true;

        } else {
            YamlConfiguration config = getRegionsConf();
            config.set("Regions." + this.regionworld + "." + this.region.getId() + ".kind", regionkind.getName());
            saveRegionsConf(config);
            this.regionKind = regionkind;
            return true;
        }
    }

    public static List<Region> getRegionList() {
        return regionList;
    }

    public static boolean setRegionKindCommand(CommandSender sender, String region, String kind){
        if(sender instanceof Player){
            if(!sender.hasPermission(Permission.ADMIN_SETREGIONKIND)) {
                sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
                return true;
            }
            Player player = (Player) sender;
            for(int i = 0; i < Region.getRegionList().size(); i++){
                if(Region.getRegionList().get(i).getRegion().getId().equalsIgnoreCase(region) && Region.getRegionList().get(i).getRegionworld().equals(player.getWorld().getName())){
                    if(Region.getRegionList().get(i).setKind(kind)){
                        sender.sendMessage(Messages.PREFIX + Messages.REGION_KIND_SET);
                        return true;
                    } else {
                        sender.sendMessage(Messages.PREFIX + Messages.REGION_KIND_NOT_EXIST);
                        return true;
                    }
                }
            }
            sender.sendMessage(Messages.PREFIX + Messages.REGION_KIND_REGION_NOT_EXIST);
            return true;
        } else {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
        }
        return true;
    }

    public static void Reset(){
        Region.regionList = new ArrayList<>();
    }

    public RegionKind getRegionKind(){
        return this.regionKind;
    }

    public static boolean listRegionKindsCommand(CommandSender sender){
        if(!sender.hasPermission(Permission.ADMIN_LISTREGIONKINDS)) {
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        sender.sendMessage(Messages.REGIONKINDS);
        for (int i = 0; i < RegionKind.getRegionKindList().size(); i++){
           sender.sendMessage("- " + RegionKind.getRegionKindList().get(i).getName());
        }
        return true;

    }

    public boolean isSold() {
        return sold;
    }

    public static void teleportToFreeRegion(RegionKind type, Player player){
        for (int i = 0; i < Region.getRegionList().size(); i++){

            if ((Region.getRegionList().get(i).isSold() == false) && (Region.getRegionList().get(i).getRegionKind() == type)){
                ProtectedRegion regionTP = Region.getRegionList().get(i).getRegion();
                Region.getRegionList().get(i).teleportToRegion(player);
                String message = Messages.REGION_TELEPORT_MESSAGE.replace("%regionid%", regionTP.getId());
                player.sendMessage(Messages.PREFIX + message);
                return;
            }
        }
        player.sendMessage(Messages.PREFIX + Messages.NO_FREE_REGION_WITH_THIS_KIND);
    }

    public static boolean teleportToFreeRegionCommand(String type, Player player){
        if(!player.hasPermission(Permission.ARM_BUYKIND + type)){
            player.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION_TO_SEARCH_THIS_KIND);
            return true;
        }
        RegionKind regionKind = RegionKind.getRegionKind(type);
        if (regionKind == null){
            player.sendMessage(Messages.PREFIX + Messages.REGIONKIND_DOES_NOT_EXIST);
            return true;
        }
        Region.teleportToFreeRegion(regionKind, player);
        return true;
    }

    public void createSchematic(){
        Main.getWorldEditInterface().createSchematic(this.getRegion(), this.getRegionworld(), Main.getWorldedit().getWorldEdit());
    }

    public boolean resetBlocks(){
        return this.resetBlocks(null);
    }

    public boolean resetBlocks(Player player){
        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File builtblocksdic = new File(pluginfolder + "/schematics/" + this.regionworld + "/" + region.getId() + "--builtblocks.schematic");
        if(builtblocksdic.exists()){
            builtblocksdic.delete();
            this.builtblocks = new ArrayList<>();
        }

        Main.getWorldEditInterface().resetBlocks(this.getRegion(), this.getRegionworld(), Main.getWorldedit().getWorldEdit());

        if(player != null) {
            player.sendMessage(Messages.PREFIX + Messages.RESET_COMPLETE);
        }
        return true;
    }

    public static Region searchRegionbyNameAndWorld(String name, String world){
        for (int i = 0; i < Region.getRegionList().size(); i++) {
            if(Region.getRegionList().get(i).getRegion().getId().equalsIgnoreCase(name) && Region.getRegionList().get(i).getRegionworld().equals(world)){
                return Region.getRegionList().get(i);
            }
        }
        return null;
    }

    public static boolean resetRegionBlocksCommand(String region, CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }

        if(!sender.hasPermission(Permission.ADMIN_RESETREGIONBLOCKS)){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        Region resregion = Region.searchRegionbyNameAndWorld(region, ((Player) sender).getPlayer().getWorld().getName());
        if(resregion == null) {
            sender.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        } else {
            resregion.resetBlocks((Player) sender);
            return true;
        }
    }

    public static boolean resetRegionCommand(String region, CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
        }

        if(!sender.hasPermission(Permission.ADMIN_RESETREGION)){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        Region resregion = Region.searchRegionbyNameAndWorld(region, ((Player) sender).getPlayer().getWorld().getName());
        if(resregion == null) {
            sender.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        } else {
            resregion.setAviable();

            resregion.resetBlocks((Player) sender);
            sender.sendMessage(Messages.PREFIX + Messages.REGION_NOW_AVIABLE);
            return true;
        }
    }

    public void regionInfo(CommandSender sender){
        String owners = "";
        String members = "";
        List<UUID> ownerslist = Main.getWorldGuardInterface().getOwners(this.getRegion());
        List<UUID> memberslist = Main.getWorldGuardInterface().getMembers(this.getRegion());
        for(int i = 0; i < ownerslist.size() - 1; i++){
            owners = owners + Bukkit.getOfflinePlayer(ownerslist.get(i)).getName() + ", ";
        }
        if(ownerslist.size() != 0){
            owners = owners + Bukkit.getOfflinePlayer(ownerslist.get(ownerslist.size() - 1)).getName();
        }
        for(int i = 0; i < memberslist.size() - 1; i++){
            members = members + Bukkit.getOfflinePlayer(memberslist.get(i)).getName() + ", ";
        }
        if(memberslist.size() != 0){
            members = members + Bukkit.getOfflinePlayer(memberslist.get(memberslist.size() - 1)).getName();
        }



        sender.sendMessage(Messages.REGION_INFO);
        sender.sendMessage(Messages.REGION_INFO_ID + this.getRegion().getId());
        sender.sendMessage(Messages.REGION_INFO_SOLD + this.isSold());
        sender.sendMessage(Messages.REGION_INFO_PRICE + this.price + " " + Messages.CURRENCY);
        sender.sendMessage(Messages.REGION_INFO_TYPE + this.getRegionKind().getName());
        sender.sendMessage(Messages.REGION_INFO_OWNER + owners);
        sender.sendMessage(Messages.REGION_INFO_MEMBERS + members);
        if(sender.hasPermission(Permission.ADMIN_INFO)){
            String autoresetmsg = Messages.REGION_INFO_AUTORESET + this.autoreset;
            if((!Main.getEnableAutoReset()) && this.autoreset){
                autoresetmsg = autoresetmsg + " (but globally disabled)";
            }
            sender.sendMessage(autoresetmsg);
            sender.sendMessage(Messages.REGION_INFO_HOTEL + this.isHotel);
            sender.sendMessage(Messages.REGION_INFO_DO_BLOCK_RESET + this.isDoBlockReset);
        }
        this.displayExtraInfo(sender);
    }

    public abstract void displayExtraInfo(CommandSender sender);

    public abstract void updateRegion();

    public static void updateRegions(){
        for(int i = 0; i < Region.getRegionList().size(); i++) {
            Region.getRegionList().get(i).updateRegion();
        }
    }

    public static boolean regionInfoCommand(CommandSender sender){
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }

        Player player = (Player) sender;

        if(!player.hasPermission(Permission.MEMBER_INFO)) {
            player.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        Location loc = (player).getLocation();

        for(int i = 0; i < Region.getRegionList().size(); i++) {
            if(Region.getRegionList().get(i).getRegion().contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) && Region.getRegionList().get(i).getRegionworld().equals(loc.getWorld().getName())) {
                Region.getRegionList().get(i).regionInfo(player);
                return true;
            }
        }
        player.sendMessage(Messages.PREFIX + Messages.HAVE_TO_STAND_ON_REGION_TO_SHOW_INFO);
        return true;
    }

    public static boolean regionInfoCommand(CommandSender sender, String regionname){
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }

        Player player = (Player) sender;

        if(!player.hasPermission(Permission.MEMBER_INFO)) {
            player.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        Region region = Region.searchRegionbyNameAndWorld(regionname, (player).getWorld().getName());

        if(region == null){
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }

        region.regionInfo(player);
        return true;
    }

    public void addMember(Player sender, String member) {
        Player playermember = Bukkit.getPlayer(member);
        if(playermember == null) {
            sender.sendMessage(Messages.PREFIX + Messages.REGION_ADD_MEMBER_NOT_ONLINE);
            return;
        }
        if(Main.getWorldGuardInterface().hasOwner((Player) sender, this.getRegion()) && sender.hasPermission(Permission.MEMBER_ADDMEMBER)) {
            Main.getWorldGuardInterface().addMember(playermember, this.getRegion());
            sender.sendMessage(Messages.PREFIX + Messages.REGION_ADD_MEMBER_ADDED);
        } else if (sender.hasPermission(Permission.ADMIN_ADDMEMBER)){
            Main.getWorldGuardInterface().addMember(playermember, this.getRegion());
            sender.sendMessage(Messages.PREFIX + Messages.REGION_ADD_MEMBER_ADDED);
        } else if (!(sender.hasPermission(Permission.MEMBER_ADDMEMBER))){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
        } else {
            sender.sendMessage(Messages.PREFIX + Messages.REGION_ADD_MEMBER_DO_NOT_OWN);
        }
    }

    public void removeMember(Player sender, String member) {

        OfflinePlayer removemember = Bukkit.getOfflinePlayer(member);

        if(Main.getWorldGuardInterface().hasOwner((Player) sender, this.getRegion()) && sender.hasPermission(Permission.MEMBER_REMOVEMEMBER)) {
            if(!(Main.getWorldGuardInterface().hasMember(removemember, this.getRegion()))) {
                sender.sendMessage(Messages.PREFIX + Messages.REGION_REMOVE_MEMBER_NOT_A_MEMBER);
                return;
            }
            Main.getWorldGuardInterface().removeMember(removemember, this.getRegion());
            sender.sendMessage(Messages.PREFIX + Messages.REGION_REMOVE_MEMBER_REMOVED);
            return;
        } else if (sender.hasPermission(Permission.ADMIN_REMOVEMEMBER)){
            if(!(Main.getWorldGuardInterface().hasMember(removemember, this.getRegion()))) {
                sender.sendMessage(Messages.PREFIX + Messages.REGION_REMOVE_MEMBER_NOT_A_MEMBER);
                return;
            }
            Main.getWorldGuardInterface().removeMember(removemember, this.getRegion());
            sender.sendMessage(Messages.PREFIX + Messages.REGION_REMOVE_MEMBER_REMOVED);
            return;
        } else if (!(sender.hasPermission(Permission.MEMBER_REMOVEMEMBER))){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return;
        } else {
            sender.sendMessage(Messages.PREFIX + Messages.REGION_REMOVE_MEMBER_DO_NOT_OWN);
            return;
        }

    }

    public static boolean addMemberCommand(CommandSender sender, String member, String regionname) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Region region = Region.searchRegionbyNameAndWorld(regionname, ((Player) sender).getWorld().getName());
        if(region == null){
            sender.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }
        region.addMember((Player) sender, member);
        return true;
    }

    public static boolean removeMemberCommand(CommandSender sender, String member, String regionname){
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Region region = Region.searchRegionbyNameAndWorld(regionname, ((Player) sender).getWorld().getName());
        if(region == null){
            sender.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }
        region.removeMember((Player) sender, member);
        return true;
    }

    public static List<Region> getRegionsByOwner(UUID uuid) {
        List<Region> regions = new LinkedList<>();
        for (int i = 0; i < Region.getRegionList().size(); i++){
            if(Main.getWorldGuardInterface().hasOwner(uuid, Region.getRegionList().get(i).getRegion())){
                regions.add(Region.getRegionList().get(i));
            }
        }
        return regions;
    }

    public static boolean autoResetRegionsFromOwner(UUID uuid){
        List<Region> regions = Region.getRegionsByOwner(uuid);
        for(int i = 0; i < regions.size(); i++){
            if(regions.get(i).getAutoreset()){
                regions.get(i).unsell();
                if(regions.get(i).isDoBlockReset()) {
                    regions.get(i).resetBlocks(null);
                }
            }
        }
        return true;
    }

    public static boolean checkIfSignExists(Sign sign) {
        for(int i = 0; i < Region.getRegionList().size(); i++){
            if(Region.getRegionList().get(i).hasSign(sign)){
                return true;
            }
        }
        return false;
    }

    public boolean getAutoreset() {
        return autoreset;
    }

    public void setAutoreset(Boolean state){
        this.autoreset = state;

        YamlConfiguration config = getRegionsConf();
        config.set("Regions." + this.regionworld + "." + this.region.getId() + ".autoreset", state);
        saveRegionsConf(config);

    }

    public static boolean changeAutoresetCommand(CommandSender sender, String regionname, String state) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }

        if(!sender.hasPermission(Permission.ADMIN_CHANGEAUTORESET)){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        Region region = Region.searchRegionbyNameAndWorld(regionname, ((Player) sender).getWorld().getName());
        if(region == null){
            sender.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }

        region.setAutoreset(Boolean.parseBoolean(state));
        String message = "disabled";

        if(Boolean.parseBoolean(state)){
            message = "enabled";
        }

        sender.sendMessage(Messages.PREFIX + "Autoreset " + message + " for " + regionname + "!");
        return true;
    }

    public Material getLogo() {
        return this.getRegionKind().getMaterial();
    }

    public void teleportToRegion(Player player) {
        if(this.teleportLocation == null) {
            World world = Bukkit.getWorld(this.regionworld);
            double coordsX = (this.getRegion().getMinimumPoint().getX() + this.getRegion().getMaximumPoint().getX()) / 2;
            double coordsZ = (this.getRegion().getMinimumPoint().getZ() + this.getRegion().getMaximumPoint().getZ()) / 2;
            for (int j = this.region.getMaximumPoint().getBlockY(); j > 0; j--) {
                Location loc = new Location(world, coordsX, j, coordsZ);
                if (!(loc.getBlock().getType() == Material.AIR) && !(loc.getBlock().getType() == Material.LAVA)) {
                    loc = new Location(world, coordsX, j + 1, coordsZ);
                    player.teleport(loc);
                //    String message = Messages.REGION_TELEPORT_MESSAGE.replace("%regionid%", this.region.getId());
                //    player.sendMessage(Messages.PREFIX + message);
                    return;
                }
            }
        } else {
            player.teleport(this.teleportLocation);
        }
    }

    public void setNewOwner(OfflinePlayer member){
        ArrayList<UUID> owner = Main.getWorldGuardInterface().getOwners(this.getRegion());
        for (int i = 0; i < owner.size(); i++) {
            Main.getWorldGuardInterface().addMember(owner.get(i), this.getRegion());
        }
        Main.getWorldGuardInterface().setOwner(member, this.getRegion());
    }

    public static List<Region> getRegionsByMember(UUID uuid) {
        List<Region> regions = new LinkedList<>();
        for (int i = 0; i < Region.getRegionList().size(); i++){
            if(Main.getWorldGuardInterface().hasMember(uuid, Region.getRegionList().get(i).getRegion())){
                regions.add(Region.getRegionList().get(i));
            }
        }
        return regions;
    }

    public String getDimensions(){
        int maxX = this.getRegion().getMaximumPoint().getBlockX();
        int maxY = this.getRegion().getMaximumPoint().getBlockY();
        int maxZ = this.getRegion().getMaximumPoint().getBlockZ();
        int minX = this.getRegion().getMinimumPoint().getBlockX();
        int minY = this.getRegion().getMinimumPoint().getBlockY();
        int minZ = this.getRegion().getMinimumPoint().getBlockZ();
        return Math.abs(maxX - minX) + "x" + Math.abs(maxY - minY) + "x" + Math.abs(maxZ - minZ);

    }

    public int timeSinceLastReset(){
        GregorianCalendar calendar = new GregorianCalendar();
        long result = calendar.getTimeInMillis() - this.lastreset;
        int days = (int) (result / (1000 * 60 * 60 * 24));
        return days;
    }

    public void userReset(Player player){
        this.resetBlocks(player);
        GregorianCalendar calendar = new GregorianCalendar();
        this.lastreset = calendar.getTimeInMillis();
        YamlConfiguration config = getRegionsConf();
        config.set("Regions." + this.regionworld + "." + this.region.getId() + ".lastreset", this.lastreset);
        saveRegionsConf(config);
    }

    public static void setResetcooldown(int cooldown){
        Region.resetcooldown = cooldown;
    }

    public static int getResetCooldown(){
        return Region.resetcooldown;
    }

    public int getRemainingDaysTillReset(){
        ArrayList<UUID> ownerlist = Main.getWorldGuardInterface().getOwners(this.getRegion());
            UUID owner = ownerlist.get(0);
            if(Main.getEnableTakeOver() ||Main.getEnableAutoReset()) {
                try{
                    ResultSet rs = Main.getStmt().executeQuery("SELECT * FROM `" + Main.getSqlPrefix() + "lastlogin` WHERE `uuid` = '" + owner.toString() + "'");

                    if(rs.next()){
                        Timestamp lastlogin = rs.getTimestamp("lastlogin");
                        GregorianCalendar calendar = new GregorianCalendar();

                        long time = lastlogin.getTime();
                        long result = calendar.getTimeInMillis() - time;
                        int days = (int)  (Main.getAutoResetAfter() - (result / (1000 * 60 * 60 * 24)));
                        return days;
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        return 0;
    }

    public void writeSigns(){
        for (int i = 0; i < this.sellsign.size(); i++) {
            this.updateSignText(this.sellsign.get(i));
        }
    }

    protected abstract void setSold(OfflinePlayer player);
    protected abstract void updateSignText(Sign mysign);
    public abstract void buy(Player player);
    public abstract void userSell(Player player);
    public abstract double getPaybackMoney();

    public void resetRegion(Player player){

        this.unsell();

        this.resetBlocks(player);
        return;
    }

    public void setAviable(){

        YamlConfiguration config = getRegionsConf();
        config.set("Regions." + this.regionworld + "." + this.getRegion().getId() + ".sold", false);
        config.set("Regions." + this.regionworld + "." + this.getRegion().getId() + ".lastreset", 1);
        saveRegionsConf(config);
        Main.getWorldGuardInterface().deleteMembers(this.getRegion());
        Main.getWorldGuardInterface().deleteOwners(this.getRegion());
        this.sold = false;
        this.lastreset = 1;

        for(int i = 0; i < this.sellsign.size(); i++){
            this.updateSignText(this.sellsign.get(i));
        }
    }

    public static boolean listRegionsCommand(CommandSender sender, String args){
        if(sender.hasPermission(Permission.ADMIN_LISTREGIONS)){
            LinkedList<String> selectedRegionsOwner = new LinkedList<>();
            LinkedList<String> selectedRegionsMember = new LinkedList<>();
            OfflinePlayer oplayer = Bukkit.getOfflinePlayer(args);
            if(oplayer == null){
                sender.sendMessage(Messages.PREFIX + "Player does not exist!");
                return true;
            }
            for(int i = 0; i < Region.getRegionList().size(); i++) {
                if(Main.getWorldGuardInterface().hasOwner(oplayer, Region.getRegionList().get(i).getRegion())){
                    selectedRegionsOwner.add(Region.getRegionList().get(i).getRegion().getId());
                }
                if(Main.getWorldGuardInterface().hasMember(oplayer, Region.getRegionList().get(i).getRegion())){
                    selectedRegionsMember.add(Region.getRegionList().get(i).getRegion().getId());
                }
            }

            String regionstring = "";
            for(int i = 0; i < selectedRegionsOwner.size() - 1; i++) {
                regionstring = regionstring + selectedRegionsOwner.get(i) + ", ";
            }
            if(selectedRegionsOwner.size() != 0){
                regionstring = regionstring + selectedRegionsOwner.get(selectedRegionsOwner.size() - 1);
            }
            sender.sendMessage(ChatColor.GOLD + "Owner: " + regionstring);

            regionstring = "";
            for(int i = 0; i < selectedRegionsMember.size() - 1; i++) {
                regionstring = regionstring + selectedRegionsMember.get(i) + ", ";
            }
            if(selectedRegionsMember.size() != 0){
                regionstring = regionstring + selectedRegionsMember.get(selectedRegionsMember.size() - 1);
            }
            sender.sendMessage(ChatColor.GOLD + "Member: " + regionstring);
            return true;


        } else {
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }
    }

    public static boolean listRegionsCommand(CommandSender sender){
        if(sender instanceof Player) {
            Player player = (Player) sender;
            if(player.hasPermission(Permission.MEMBER_LISTREGIONS)){
                Region.listRegionsCommand(player, player.getName());
                return true;


            } else {
                sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
                return true;
            }
        } else {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
    }

    public static boolean setRegionOwner(CommandSender sender, String regionString, String ownerString){

        if(!sender.hasPermission(Permission.ADMIN_SETOWNER)) {
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player playersender = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionString, playersender.getWorld().getName());
        if (region == null) {
            sender.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(ownerString);

        if (player == null) {
            playersender.sendMessage(Messages.PREFIX + "Player not found!");
            return true;
        }

        region.setSold(player);
        sender.sendMessage(Messages.PREFIX + "Owner set!");
        return true;
    }

    public boolean isHotel() {
        return isHotel;
    }

    public boolean allowBlockBreak(Location breakloc) {
        if(this.isHotel) {
            for (int i = 0; i < this.builtblocks.size(); i++) {
                if (this.builtblocks.get(i).distance(breakloc) < 1) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public void setHotel(Boolean bool) {
        this.isHotel = bool;
        YamlConfiguration config = getRegionsConf();
        config.set("Regions." + this.regionworld + "." + this.getRegion().getId() + ".isHotel", bool);
        saveRegionsConf(config);
    }

    public static boolean setHotel(CommandSender sender, String regionString, String booleanstring){
        if(!sender.hasPermission(Permission.ADMIN_CHANGE_IS_HOTEL)){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }
        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionString, player.getWorld().getName());
        if(region == null) {
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }
        region.setHotel(Boolean.parseBoolean(booleanstring));
        String state = "disabled";
        if(Boolean.parseBoolean(booleanstring)){
            state = "enabled";
        }
        player.sendMessage(Messages.PREFIX + "isHotel " + state + " for " + region.getRegion().getId());
        return true;
    }

    public static boolean createNewSchematic(CommandSender sender, String regionString) {
        if(!sender.hasPermission(Permission.ADMIN_UPDATESCHEMATIC)){
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }
        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionString, player.getWorld().getName());

        if(region == null) {
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }
        player.sendMessage(Messages.PREFIX + "Creating schematic...");
        region.createSchematic();
        player.sendMessage(Messages.PREFIX + Messages.COMPLETE);
        return true;
    }

    public static boolean teleport(CommandSender sender, String regionString) {
        if (!sender.hasPermission(Permission.ADMIN_TP)) {
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionString, player.getWorld().getName());

        if (region == null) {
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }
        region.teleportToRegion(player);
        return true;
    }

    public static void generatedefaultConfig(){
        Plugin plugin = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket");
        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File messagesdic = new File(pluginfolder + "/regions.yml");
        if(!messagesdic.exists()){
            try {
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                InputStream stream = plugin.getResource("regions.yml");
                byte[] buffer = new byte[stream.available()];
                stream.read(buffer);
                OutputStream output = new FileOutputStream(messagesdic);
                output.write(buffer);
                output.close();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static YamlConfiguration getRegionsConf() {
        return Region.regionsconf;
    }

    public static void setRegionsConf(){
        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File regionsconfigdic = new File(pluginfolder + "/regions.yml");
        Region.regionsconf = YamlConfiguration.loadConfiguration(regionsconfigdic);
    }

    public static void saveRegionsConf(YamlConfiguration conf) {
        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File regionsconfigdic = new File(pluginfolder + "/regions.yml");
        try {
            conf.save(regionsconfigdic);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPaypackPercentage(double percent){
        Region.paybackPercentage = percent;
    }

    public static boolean setTeleportLocation(String regionName, CommandSender sender){
        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionName, player.getWorld().getName());
        if(region == null){
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }

        region.setTeleportLocation(player.getLocation());

        player.sendMessage(Messages.PREFIX + "Warp set!");
        return true;
    }

    public static boolean unsellCommand(String regionName, CommandSender sender){
        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionName, player.getWorld().getName());
        if(region == null){
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }

        region.unsell();

        player.sendMessage(Messages.PREFIX + Messages.REGION_NOW_AVIABLE);
        return true;
    }

    public void unsell(){
        this.setAviable();
        YamlConfiguration config = getRegionsConf();
        config.set("Regions." + this.regionworld + "." + this.getRegion().getId() + ".sold", false);
        saveRegionsConf(config);

        Main.getWorldGuardInterface().deleteOwners(this.getRegion());
        Main.getWorldGuardInterface().deleteMembers(this.getRegion());
    }

    public static boolean extendCommand(String regionName, CommandSender sender){
        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionName, player.getWorld().getName());
        if(region == null){
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }

        if(!(region instanceof RentRegion)) {
            player.sendMessage(Messages.PREFIX + Messages.REGION_IS_NOT_A_RENTREGION);
            return true;
        }

        ((RentRegion) region).extendRegion(player);

        return true;
    }

    public static boolean deleteCommand(String regionName, CommandSender sender){
        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionName, player.getWorld().getName());
        if(region == null){
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }

        region.unsell();
        for(int i = 0; i < region.sellsign.size(); i++) {
            Location loc = region.sellsign.get(i).getLocation();
            loc.getBlock().setType(Material.AIR);
            region.removeSign(loc, null);
            i--;
        }

        player.sendMessage(Messages.PREFIX + region.getRegion().getId() + " deleted!");
        return true;
    }

    public boolean isDoBlockReset() {
        return isDoBlockReset;
    }

    public static boolean setDoBlockResetCommand(String regionName, String setting, CommandSender sender){
        if(!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.COMMAND_ONLY_INGAME);
            return true;
        }
        Player player = (Player) sender;
        Region region = Region.searchRegionbyNameAndWorld(regionName, player.getWorld().getName());
        if(region == null){
            player.sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
            return true;
        }

        Boolean boolsetting = Boolean.parseBoolean(setting);

        region.setDoBlockReset(boolsetting);

        player.sendMessage(Messages.PREFIX + "Setted doBlockReset for " + region.getRegion().getId() + " to " + setting);
        return true;
    }

    public void setDoBlockReset(Boolean bool) {
        this.isDoBlockReset = bool;
        YamlConfiguration config = getRegionsConf();
        config.set("Regions." + this.regionworld + "." + this.getRegion().getId() + ".doBlockReset", bool);
        saveRegionsConf(config);
    }
}

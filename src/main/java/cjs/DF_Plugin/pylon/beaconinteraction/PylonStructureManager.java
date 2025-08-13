package cjs.DF_Plugin.pylon.beaconinteraction;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class PylonStructureManager {

    public void placeBaseAndBarrier(Location beaconLocation) {
        placeIronBase(beaconLocation);
        placeVerticalBarrier(beaconLocation);
    }

    public void removeBaseAndBarrier(Location beaconLocation) {
        removeIronBase(beaconLocation);
        removeVerticalBarrier(beaconLocation);
    }

    /**
     * 파일런의 기반(철 블록)만 설치합니다.
     * @param beaconLocation 파일런의 위치
     */
    public void placeBaseOnly(Location beaconLocation) {
        placeIronBase(beaconLocation);
    }

    private void placeIronBase(Location beaconLocation) {
        Location baseCenter = beaconLocation.clone().subtract(0, 1, 0);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                baseCenter.clone().add(x, 0, z).getBlock().setType(Material.IRON_BLOCK);
            }
        }
    }

    private void placeVerticalBarrier(Location beaconLocation) {
        World world = beaconLocation.getWorld();
        if (world == null) return;
        for (int y = beaconLocation.getBlockY() + 1; y < world.getMaxHeight(); y++) {
            new Location(world, beaconLocation.getX(), y, beaconLocation.getZ()).getBlock().setType(Material.BARRIER);
        }
    }

    private void removeIronBase(Location beaconLocation) {
        Location baseCenter = beaconLocation.clone().subtract(0, 1, 0);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block block = baseCenter.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.IRON_BLOCK) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private void removeVerticalBarrier(Location beaconLocation) {
        World world = beaconLocation.getWorld();
        if (world == null) return;
        for (int y = beaconLocation.getBlockY() + 1; y < world.getMaxHeight(); y++) {
            Block block = new Location(world, beaconLocation.getX(), y, beaconLocation.getZ()).getBlock();
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR);
            }
        }
    }
}
package com.plushnode.chissentials;

import com.plushnode.chissentials.collision.AABB;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PrecisionSpot {
    public enum PrecisionArea {
        KneeLeft,
        KneeRight,
        ShoulderLeft,
        ShoulderRight
    }

    private static final double BOUNDS_WIDTH = 0.3;
    private static final double HALF_BOUNDS_WIDTH = BOUNDS_WIDTH / 2;
    public static final AABB BASE_BOUNDS = new AABB(
            new Vector(-HALF_BOUNDS_WIDTH, -HALF_BOUNDS_WIDTH, -HALF_BOUNDS_WIDTH),
            new Vector(HALF_BOUNDS_WIDTH, HALF_BOUNDS_WIDTH, HALF_BOUNDS_WIDTH)
    );

    // The area of the body that this precision spot is for.
    private PrecisionArea area;

    // Bounding box in world coords.
    private AABB bounds;

    public PrecisionSpot(AABB bounds, PrecisionArea area) {
        this.bounds = bounds;
        this.area = area;
    }

    public PrecisionArea getArea() {
        return area;
    }

    public AABB getBounds() {
        return this.bounds;
    }

    public static PrecisionSpot getPrecisionSpot(Player player, PrecisionArea area) {
        Location location = player.getLocation();
        Vector front = location.getDirection();

        Vector up = new Vector(0, 1, 0);
        Vector right = front.crossProduct(up).normalize();

        return getPrecisionSpot(player, area, up, right);
    }

    public static PrecisionSpot getPrecisionSpot(Player player, PrecisionArea area, Vector up, Vector right) {
        Location location = player.getLocation();

        Vector kneeHeight = up.clone().multiply(0.5);
        Vector shoulderHeight = up.clone().multiply(1.25);

        Vector position;

        switch (area) {
            case KneeRight:
                position = location.toVector().add(kneeHeight).add(right.clone().multiply(0.2));
                break;
            case KneeLeft:
                position = location.toVector().add(kneeHeight).add(right.clone().multiply(-0.2));
                break;
            case ShoulderRight:
                position = location.toVector().add(shoulderHeight).add(right.clone().multiply(0.3));
                break;
            case ShoulderLeft:
                position = location.toVector().add(shoulderHeight).add(right.clone().multiply(-0.3));
                break;
            default:
                position = new Vector(0, 0, 0);
        }

        return new PrecisionSpot(PrecisionSpot.BASE_BOUNDS.at(position), area);
    }
}

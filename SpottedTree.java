package spacegavinplayer;

import battlecode.common.TreeInfo;

/**
 * Created by jack on 1/11/2017.
 */
public class SpottedTree extends SpottedItem{
    public final TreeInfo spottedInformation;
    public final boolean hasRobot;

    public SpottedTree(int turnSpotted, TreeInfo spottedInformation, boolean hasRobot) {
        super(turnSpotted);
        this.spottedInformation = spottedInformation;
        this.hasRobot = hasRobot;
    }
}

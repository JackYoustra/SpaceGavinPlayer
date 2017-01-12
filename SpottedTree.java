package spacegavinplayer;

import battlecode.common.TreeInfo;

/**
 * Created by jack on 1/11/2017.
 */
public class SpottedTree extends SpottedItem{
    public final TreeInfo spottedInformation;

    public SpottedTree(int turnSpotted, TreeInfo spottedInformation) {
        super(turnSpotted);
        this.spottedInformation = spottedInformation;
    }
}

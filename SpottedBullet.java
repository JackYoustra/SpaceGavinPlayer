package spacegavinplayer;

import battlecode.common.BulletInfo;
import battlecode.common.RobotInfo;

/**
 * Created by jack on 1/11/2017.
 */
public class SpottedBullet extends SpottedItem{
    public final BulletInfo spottedInformation;

    public SpottedBullet(int turnSpotted, BulletInfo spottedInformation) {
        super(turnSpotted);
        this.spottedInformation = spottedInformation;
    }

    float getPossibleDistanceRadius(int currentTurn){
        return (currentTurn-turnSpotted) * spottedInformation.speed;
    }
}

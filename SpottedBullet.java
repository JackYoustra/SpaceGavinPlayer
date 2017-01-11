package spacegavinplayer;

import battlecode.common.BulletInfo;
import battlecode.common.RobotInfo;

/**
 * Created by jack on 1/11/2017.
 */
public class SpottedBullet {
    public final int turnSpotted;
    public final BulletInfo spottedInformation;

    public SpottedBullet(int turnSpotted, BulletInfo spottedInformation) {
        this.turnSpotted = turnSpotted;
        this.spottedInformation = spottedInformation;
    }

    float getPossibleDistanceRadius(int currentTurn){
        return (currentTurn-turnSpotted) * spottedInformation.speed;
    }
}

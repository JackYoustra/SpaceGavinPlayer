package spacegavinplayer;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/**
 * Created by jack on 1/11/2017.
 */
public class SpottedRobot { // different class for spotted bullets
    public final int turnSpotted;
    public final RobotInfo spottedInformation;

    public SpottedRobot(int turnSpotted, RobotInfo spottedInformation) {
        this.turnSpotted = turnSpotted;
        this.spottedInformation = spottedInformation;
    }

    float getPossibleDistanceRadius(int currentTurn){
        return (currentTurn-turnSpotted) * RobotStats.movement(spottedInformation.type);
    }
}

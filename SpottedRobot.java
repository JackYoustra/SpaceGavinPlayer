package spacegavinplayer;

import battlecode.common.RobotInfo;

/**
 * Created by jack on 1/11/2017.
 */
public class SpottedRobot extends SpottedItem{ // different class for spotted bullets
    public final RobotInfo spottedInformation;

    public SpottedRobot(int turnSpotted, RobotInfo spottedInformation) {
        super(turnSpotted);
        this.spottedInformation = spottedInformation;
    }

    float getPossibleDistanceRadius(int currentTurn){
        return (currentTurn-turnSpotted) * spottedInformation.type.strideRadius;
    }

    @Override
    public String toString() {
        return "ID: " + spottedInformation.ID + ", Team: " + spottedInformation.team;
    }
}

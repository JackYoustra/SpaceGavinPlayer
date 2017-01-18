package spacegavinplayer;

import battlecode.common.RobotInfo;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpottedRobot that = (SpottedRobot) o;
        return spottedInformation.ID == that.spottedInformation.ID && spottedInformation.team == that.spottedInformation.team;
    }

    @Override
    public int hashCode() {
        return Objects.hash(spottedInformation);
    }

    @Override
    public String toString() {
        return "ID: " + spottedInformation.ID + ", Team: " + spottedInformation.team;
    }
}

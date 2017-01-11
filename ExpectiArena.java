package spacegavinplayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jack on 1/11/2017.
 */
public class ExpectiArena {
    private Map<Integer, SpottedRobot> spottedRobots = new HashMap<>(); // UUID to robot

    public void updateRobot(SpottedRobot robot){
        spottedRobots.put(robot.spottedInformation.ID, robot);
    }

    public SpottedRobot[] getRobots(){
        final Collection<SpottedRobot> values = spottedRobots.values();
        return values.toArray(new SpottedRobot[values.size()]);
    }

}

package spacegavinplayer;

import battlecode.common.RobotType;

/**
 * Created by jack on 1/11/2017.
 */
public class RobotStats {
    static float movement(RobotType type){
        switch (type){
            case ARCHON:
                return 1;
            case GARDENER:
                return 1;
            case LUMBERJACK:
                return 1.5f;
            case SOLDIER:
                return 2;
            case SCOUT:
                return 2.5f;
            case TANK:
                return 1;
        }
        return 1;
    }
}

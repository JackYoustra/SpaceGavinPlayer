package spacegavinplayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jack on 1/11/2017.
 */
public class ExpectiArena {
    private Map<Integer, SpottedRobot> spottedRobots = new HashMap<>(); // UUID of robot : robot
    private Map<Integer, SpottedBullet> spottedBullets = new HashMap<>(); // UUID of bullet : bullet
    private Map<Integer, SpottedTree> spottedTrees = new HashMap<>(); // UUID of tree : tree

    // TOOD: Allow for destruction of objects

    public void updateRobot(SpottedRobot robot){
        spottedRobots.put(robot.spottedInformation.ID, robot);
    }

    public SpottedRobot[] getRobots(){
        final Collection<SpottedRobot> values = spottedRobots.values();
        return values.toArray(new SpottedRobot[values.size()]);
    }

    public void updateBullet(SpottedBullet bullet){
        spottedBullets.put(bullet.spottedInformation.ID, bullet);
    }

    public SpottedBullet[] getBullets(){
        final Collection<SpottedBullet> values = spottedBullets.values();
        return values.toArray(new SpottedBullet[values.size()]);
    }

    public void updateTree(SpottedTree tree){
        spottedTrees.put(tree.spottedInformation.ID, tree);
    }

    public SpottedTree[] getTrees(){
        final Collection<SpottedTree> values = spottedTrees.values();
        return values.toArray(new SpottedTree[values.size()]);
    }


    public boolean isUpdated(SpottedRobot srobot, int turn) {
        final SpottedRobot spottedRobot = spottedRobots.get(srobot);
        if(spottedRobot != null && spottedRobot.turnSpotted == turn){
            return true;
        }
        return false;
    }

    public boolean isUpdated(SpottedBullet sbullet, int turn) {
        final SpottedBullet spottedBullet = spottedBullets.get(sbullet);
        if(spottedBullet != null && spottedBullet.turnSpotted == turn){
            return true;
        }
        return false;
    }

    public boolean isUpdated(SpottedTree stree, int turn) {
        final SpottedTree spottedTree = spottedTrees.get(stree);
        if(spottedTree != null && spottedTree.turnSpotted == turn){
            return true;
        }
        return false;
    }
}

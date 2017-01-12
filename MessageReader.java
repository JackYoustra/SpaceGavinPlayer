package spacegavinplayer;

import battlecode.common.*;

import java.util.ArrayList;

/**
 * Created by jack on 1/11/2017.
 */
public class MessageReader {
    /*
    Messages:
    first int is header, has timestamp in MS bits (12 with assumption of 3k max rounds), robot type / tree type in LS bits (5 bits total). Bits 6-14 are health
     */

    static int writeLocation = -1;
    private static final int ROBOT_TYPE_MASK =      0b00111; //can be 0-5, so 3 bits of info
    private static final int TREE_FLAG =            0b01000;
    private static final int TEAM_A_FLAG =          0b10000;


    public static class Inbox{
        public static SpottedItem[] items;
        public static int aggressionslider;
        public static int vpslider;
    }

    static void updateInbox(RobotController rc){
        ArrayList<SpottedItem> items = new ArrayList<>();
        int position = 0; // always start at beginning to read
        while(true){
            // read item
            int leading_packet = 0;
            try {
                leading_packet = rc.readBroadcast(position);
            }catch(GameActionException e){
                System.err.println("out of bounds broadcast");
                e.printStackTrace();
            }
            if(rc.getRoundNum() == (leading_packet >>> 32-12)){ // get most significant 12 bits (bits 20-32)
                // "fresh" message, proceed
                int flattenedX = 0;
                // read position data, rest of run packet
                try {
                    flattenedX = rc.readBroadcast(position+1);
                } catch (GameActionException e) {
                    System.err.println("out of bounds broadcast");
                    e.printStackTrace();
                }
                float xCoordinate = Float.intBitsToFloat(flattenedX);

                int flattenedY = 0;
                // read position data
                try {
                    flattenedY = rc.readBroadcast(position+2);
                } catch (GameActionException e) {
                    System.err.println("out of bounds broadcast");
                    e.printStackTrace();
                }
                float yCoordinate = Float.intBitsToFloat(flattenedY);

                int id = 0;
                try {
                    id = rc.readBroadcast(position+3);
                } catch (GameActionException e) {
                    e.printStackTrace();
                }

                // get health
                int health = (leading_packet>>6)&0xFF; // bits 6-14

                // get team
                Team playerTeam = Team.A;
                if((TEAM_A_FLAG & leading_packet) == 0){
                    playerTeam = Team.B;
                }

                // get ordinal of rc or tree
                SpottedItem item = null;
                if((leading_packet & TREE_FLAG) == 0) {
                    int robot_type_bits = leading_packet & ROBOT_TYPE_MASK;
                    RobotType type = RobotType.values()[robot_type_bits];
                    item = new SpottedRobot(rc.getRoundNum(), new RobotInfo(id, playerTeam, type, new MapLocation(xCoordinate, yCoordinate), health, -1, -1)); // not counting attack and move
                }
                else{
                    // is tree

                }

                // do final increment of position
                position += 4; // one meta, two coordinates

            }
            else{
                // mark starting point for writing when need to, this is first encounter into obselete messages
                writeLocation = position;
                break;
            }
        }
        // read meta slot
        int metacode = 0;
        try {
            metacode = rc.readBroadcast(GameConstants.BROADCAST_MAX_CHANNELS);
        } catch (GameActionException e) {
            System.err.println("out of bounds broadcast");
            e.printStackTrace();
        }

        // update inbox
        Inbox.aggressionslider = (metacode>>0)&0xFF; // first 8 bits
        Inbox.vpslider = (metacode>>8)&0xFF; // bits 8-16
        Inbox.items = items.toArray(new SpottedTree[items.size()]);
    }
}

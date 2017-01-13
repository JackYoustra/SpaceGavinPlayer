package spacegavinplayer;

import battlecode.common.*;

import java.util.ArrayList;

import static spacegavinplayer.RobotPlayer.rc;

/**
 * Created by jack on 1/11/2017.
 */
public class MessageReader {
    /*
    Messages:
    first int is header, has timestamp in MS bits (12 with assumption of 3k max rounds), robot type / tree type in LS bits (5 bits total). Bits 6-14 are health
     */

    static int writeLocation = -1; // seek head, start at reading section and end to convert to write head
    private static final int ROBOT_TYPE_MASK =      0b00111; //can be 0-5, so 3 bits of info
    private static final int TREE_FLAG =            0b01000;
    private static final int TEAM_A_FLAG =          0b10000;


    public static class Inbox{
        public static SpottedItem[] items = new SpottedItem[0];
        public static int aggressionslider = 0;
        public static int vpslider = 0;
    }

    static void updateInbox(){
        ArrayList<SpottedItem> items = new ArrayList<>();
        int position = 0; // always start at beginning to read
        try {
            while(true){
                // read item
                int leading_packet = 0;
                    leading_packet = rc.readBroadcast(position);
                if(leading_packet == 0){
                    // first round, nothing needed
                    return;
                }
                if(rc.getRoundNum() == (leading_packet >>> 32-12)){ // get most significant 12 bits (bits 20-32)
                    // "fresh" message, proceed
                    int flattenedX = 0;
                    // read position data, rest of run packet
                    flattenedX = rc.readBroadcast(position+1);
                    float xCoordinate = Float.intBitsToFloat(flattenedX);

                    int flattenedY = 0;
                    // read position data
                    flattenedY = rc.readBroadcast(position+2);
                    float yCoordinate = Float.intBitsToFloat(flattenedY);

                    int third_packet = 0;
                    try {
                        third_packet = rc.readBroadcast(position+3);
                    } catch (GameActionException e) {
                        e.printStackTrace();
                    }

                    int id = third_packet&0xFFFF; // first 16 bits

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
                        boolean hasRobot = false;
                        if((third_packet&0b10000000000000000000000000000000) != 0) hasRobot = true; // check last bit
                        // bits 17-31 are bullet numbers
                        int bullet_number = (third_packet >> 17) & 0x7FFF; // 15 bit uint
                        item = new SpottedTree(rc.getRoundNum(), new TreeInfo(id, playerTeam, new MapLocation(xCoordinate, yCoordinate), -1, health, bullet_number, null), hasRobot);
                    }
                    items.add(item);
                    // do final increment of position
                    position += 4; // one meta, two coordinates, one id

                }
                else{
                    // mark starting point for writing when need to, this is first encounter into obselete messages
                    writeLocation = position;
                    break;
                }
            }
            // read meta slot
            int metacode = 0;
            metacode = rc.readBroadcast(GameConstants.BROADCAST_MAX_CHANNELS-1);
            // update inbox
            Inbox.aggressionslider = (metacode>>0)&0xFF; // first 8 bits
            Inbox.vpslider = (metacode>>8)&0xFF; // bits 8-16
            Inbox.items = items.toArray(new SpottedTree[items.size()]);
        } catch (GameActionException e) {
            System.err.println("out of bounds broadcast");
            e.printStackTrace();
        }
    }

    static void write(){

    }
}

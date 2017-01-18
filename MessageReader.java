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

    static int writeLocation = 0; // seek head, start at reading section and end to convert to write head
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
                int leading_packet = RobotPlayer.rc.readBroadcast(position);
                if(leading_packet == 0){
                    // nothing in packet
                    return;
                }
                if(RobotPlayer.rc.getRoundNum() == (leading_packet >>> (32-12))){ // get most significant 12 bits (bits 20-32)
                    // "fresh" message, proceed
                    int flattenedX = 0;
                    // read position data, rest of run packet
                    flattenedX = RobotPlayer.rc.readBroadcast(position+1);
                    float xCoordinate = Float.intBitsToFloat(flattenedX);

                    int flattenedY = 0;
                    // read position data
                    flattenedY = RobotPlayer.rc.readBroadcast(position+2);
                    float yCoordinate = Float.intBitsToFloat(flattenedY);

                    int third_packet = 0;
                    try {
                        third_packet = RobotPlayer.rc.readBroadcast(position+3);
                    } catch (GameActionException e) {
                        e.printStackTrace();
                    }

                    int id = third_packet&0xFFFF; // first 16 bits
                    System.out.println("ID: " + id + "; Packet: " + position);

                    // get health
                    int health = (leading_packet>>>6)&0xFF; // bits 6-14

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
                        item = new SpottedRobot(RobotPlayer.rc.getRoundNum(), new RobotInfo(id, playerTeam, type, new MapLocation(xCoordinate, yCoordinate), health, -1, -1)); // not counting attack and move
                    }
                    else{
                        // is tree
                        boolean hasRobot = false;
                        if((third_packet&0b10000000000000000000000000000000) != 0) hasRobot = true; // check last bit
                        // bits 17-31 are bullet numbers
                        int bullet_number = (third_packet >>> 17) & 0x7FFF; // 15 bit uint
                        item = new SpottedTree(RobotPlayer.rc.getRoundNum(), new TreeInfo(id, playerTeam, new MapLocation(xCoordinate, yCoordinate), -1, health, bullet_number, null), hasRobot);
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
            int metacode = RobotPlayer.rc.readBroadcast(GameConstants.BROADCAST_MAX_CHANNELS-1);
            // update inbox
            Inbox.aggressionslider = (metacode)&0xFF; // first 8 bits
            Inbox.vpslider = (metacode>>>8)&0xFF; // bits 8-16
            Inbox.items = items.toArray(new SpottedItem[items.size()]);
        } catch (GameActionException e) {
            System.err.println("out of bounds broadcast");
            e.printStackTrace();
        }
    }

    static void write(SpottedRobot robot){
        // metadata
        int leading_packet = RobotPlayer.rc.getRoundNum() << (32-12); // shift turn into most significant 12 bits
        if(RobotPlayer.rc.getTeam() == Team.A){
            leading_packet |= TEAM_A_FLAG;
        }
        leading_packet |= ((int)robot.spottedInformation.health) << 6; // truncate health
        leading_packet |= robot.spottedInformation.type.ordinal(); // no shift necessary, bits 0-3

        // position data
        final int second_packet = Float.floatToIntBits(robot.spottedInformation.location.x);
        final int third_packet = Float.floatToIntBits(robot.spottedInformation.location.y);

        // id packet
        final int id_packet = robot.spottedInformation.ID;

        try {
            RobotPlayer.rc.broadcast(writeLocation, leading_packet); // won't be off by one
            RobotPlayer.rc.broadcast(writeLocation + 1, second_packet);
            RobotPlayer.rc.broadcast(writeLocation + 2, third_packet);
            RobotPlayer.rc.broadcast(writeLocation + 3, id_packet);
            writeLocation+=4;
        } catch(GameActionException e){
            System.err.println("error writing broadcast");
            e.printStackTrace();
        }
    }
}

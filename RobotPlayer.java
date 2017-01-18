package spacegavinplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    public static final int MAX_DODGE_BYTECODE = 4000;
    public static final double COST_CUTOFF = 0.01;
    static RobotController rc;
    static ExpectiArena arena = new ExpectiArena();
    public static float ROBOT_SPEED;
    public static float BODY_RADIUS;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        ROBOT_SPEED = rc.getType().strideRadius; // single lookup
        BODY_RADIUS = rc.getType().bodyRadius;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        try {
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case TANK:
                runTank();
                break;
            case SCOUT:
                runScout();
                break;
        }
        }catch (Exception e){
            System.err.println("Unhandled exception!!!");
            e.printStackTrace();
        }
	}

    static void runCommon(){
        System.out.println("in common");
        // read from message cache & update map, reset seeker head first
        updateFromInbox();
        System.out.println("Post-inbox, cycles left: " + Clock.getBytecodesLeft());
        sense();
        System.out.println("Post-sense, cycles left: " + Clock.getBytecodesLeft());
        dodge();
        System.out.println("Post-dodge, cycles left: " + Clock.getBytecodesLeft());
        //dump();
        // transmit - new enemies, trees
    }

    static void dump(){
        try {
            for (int i = 0; i < 20; i++) {
                System.out.println(i + ": " + rc.readBroadcast(i));
            }
        }catch(GameActionException e){

        }
    }

    // returns whether or not dodged
    static void dodge(){
        BulletInfo[] scannedbullets = rc.senseNearbyBullets(); // get bullets to dodge
        if(scannedbullets.length == 0) return; // rest if none
        /* refined quadrant check. Voronay diagram algorithm (I think) where split
        it into four quadrants, find the best one, and continue spitting it. Basically a sampling algo
         */

        final MapLocation playerLocation = rc.getLocation();
        BulletInfo[] bullets = new BulletInfo[scannedbullets.length];
        // prune bullets heading away from robot, go through first
        int counter = 0;
        for (int i = scannedbullets.length; --i >= 0;) {
            final BulletInfo info = scannedbullets[i];
            final MapLocation bulletLocation = info.getLocation();
            Direction directionToRobot = bulletLocation.directionTo(playerLocation);
            //float distToRobot = bulletLocation.distanceTo(playerLocation);
            final float nextDistance = bulletLocation.add(info.dir, info.speed).distanceTo(playerLocation);
            float theta = info.dir.radiansBetween(directionToRobot);
            // If theta > 90 degrees, then the bullet is traveling away from us and we can break early so long as it's also not in movement footprint (size + move speed). Basically, worst-case can't hit us. The following if is a negation of the case to prevent an empty if and only an else clause thing
            if (!(Math.abs(theta) > Math.PI/2) || ROBOT_SPEED + BODY_RADIUS > nextDistance) {
                bullets[counter++] = info;
            }
        }
        System.out.println("Sensed: " + scannedbullets.length + ", Incoming: " + counter);

        MapLocation centeredLocation = new MapLocation(playerLocation.x, playerLocation.y);
        BulletInfo[] relevantBullets = new BulletInfo[bullets.length];
        float stride_length = ROBOT_SPEED / 2.0f; // start at origin and have space to move as far as needed to either side, could start farther away if necessary
        Direction travel = Direction.getNorth();
        double mostRelaxedClosestFitDistance = Double.MIN_VALUE;
        double smallestDistance = Double.MAX_VALUE; // basically want to find smallest distance in the group (closest call) and then get the largest distance from there. TODO: Average in the future instead of shortest?
        // expend 1k bytecode max on gradient search
        final int currentBytecodes = Clock.getBytecodeNum();
        int relevantBulletsIndex = 0;
        do{
            boolean relevantInserted = false; // retention for relevancy (not a wild miss) only needs to happen once in following
            relevantBulletsIndex = 0;
            // if no bullets, do nothing
            MapLocation testLocation = centeredLocation.add(Direction.getNorth(), stride_length);
            if(rc.canMove(testLocation)) {
                for (BulletInfo bullet : bullets) {
                    if (bullet == null) break;
                    double distance = cost(testLocation, bullet);
                    if (distance > COST_CUTOFF) {
                        relevantBullets[relevantBulletsIndex++] = bullet;
                        relevantInserted = true;
                    }
                    // consider using fast inverse square root for distance: http://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
                    if (smallestDistance > distance) {
                        smallestDistance = distance;
                    }
                }
                mostRelaxedClosestFitDistance = smallestDistance;
                System.out.println("North most relaxed: " + mostRelaxedClosestFitDistance);
            }
            testLocation = centeredLocation.add(Direction.getEast(), stride_length);
            if(rc.canMove(testLocation)) {
                smallestDistance = Double.MAX_VALUE;
                for (BulletInfo bullet : bullets) {
                    if (bullet == null) break;
                    double distance = cost(testLocation, bullet);
                    if (distance > COST_CUTOFF && !relevantInserted) {
                        relevantBullets[relevantBulletsIndex++] = bullet;
                        relevantInserted = true;
                    }
                    // consider using fast inverse square root for distance: http://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
                    if (smallestDistance > distance) {
                        smallestDistance = distance;
                    }
                }
                System.out.println("East most relaxed: " + smallestDistance);
                if (mostRelaxedClosestFitDistance < smallestDistance) {
                    mostRelaxedClosestFitDistance = smallestDistance;
                    travel = Direction.getEast();
                }
            }
            testLocation = centeredLocation.add(Direction.getSouth(), stride_length);
            if(rc.canMove(testLocation)) {
                smallestDistance = Double.MAX_VALUE;
                for (BulletInfo bullet : bullets) {
                    if (bullet == null) break;
                    double distance = cost(testLocation, bullet);
                    if (distance > COST_CUTOFF && !relevantInserted) {
                        relevantBullets[relevantBulletsIndex++] = bullet;
                        relevantInserted = true;
                    }
                    // consider using fast inverse square root for distance: http://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
                    if (smallestDistance > distance) {
                        smallestDistance = distance;
                    }
                }
                System.out.println("South most relaxed: " + smallestDistance);
                if (mostRelaxedClosestFitDistance < smallestDistance) {
                    mostRelaxedClosestFitDistance = smallestDistance;
                    travel = Direction.getSouth();
                }
            }

            testLocation = centeredLocation.add(Direction.getWest(), stride_length);
            if(rc.canMove(testLocation)) {
                smallestDistance = Double.MAX_VALUE;
                for (BulletInfo bullet : bullets) {
                    if (bullet == null) break;
                    double distance = cost(testLocation, bullet);
                    if (distance > COST_CUTOFF && !relevantInserted) {
                        relevantBullets[relevantBulletsIndex++] = bullet;
                        relevantInserted = true;
                    }
                    // consider using fast inverse square root for distance: http://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
                    if (smallestDistance > distance) {
                        smallestDistance = distance;
                    }
                }
                System.out.println("West most relaxed: " + smallestDistance);
                if (mostRelaxedClosestFitDistance < smallestDistance) {
                    mostRelaxedClosestFitDistance = smallestDistance;
                    travel = Direction.getWest();
                }
            }
            if(mostRelaxedClosestFitDistance != Double.MIN_VALUE){
                // if not, none of the places could be moved towards, so stay in place and just reduce stride
                // make "movement" and halve step size
                centeredLocation = centeredLocation.add(travel, stride_length); // final choice - but could also stick with the best testLocation
                // reduce bullets
                System.arraycopy(relevantBullets, 0, bullets, 0, relevantBullets.length); // need to overwrite values with nulls too
                // relevantBulletsIndex = 0; set above so use in conditional
            }
            stride_length /= 2.0f;

        }while(relevantBulletsIndex > 0 && Clock.getBytecodeNum() - currentBytecodes < MAX_DODGE_BYTECODE);
        System.out.println(centeredLocation.x + ", " + centeredLocation.y);
        System.out.println(playerLocation.x + ", " + playerLocation.y);
        if(centeredLocation.x == playerLocation.x && centeredLocation.y == playerLocation.y) {
            // no movement expended dodging if not needed
        }
        else{
            // finally make movement
            try {
                rc.move(centeredLocation);
            } catch (GameActionException e) {
                System.err.println("Invalid move!");
                e.printStackTrace();
            }
        }
    }

    static double cost(MapLocation centeredLocation, BulletInfo bullet) {
        final MapLocation bulletLocation = bullet.getLocation();
        // a = -1, b = -slope
        final double b = Math.tan(bullet.dir.radians) * -1.0;
        final double c = (b * -1.0) * bulletLocation.y + bulletLocation.x;
        final double asquaredbsquared = b*b; // -1s cancel out
        // first, find the closest x will get to the robot's location.
        // https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line and http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
        final double inner = b * bulletLocation.x + bulletLocation.y;
        /* Don't need these for now:
        double closestX = (b * inner + c) /
                asquaredbsquared;
        double closestY = (inner - b*c) /
                asquaredbsquared; // can reuse inner because a is merely negation
                */
        double distance =  Math.abs(-1 *centeredLocation.x + b*centeredLocation.y + c) /
                                Math.sqrt(asquaredbsquared);
        //double cost = bullet.speed/distance; // if close and fast, is huge, if far and slow, is low
        double cost = (distance + centeredLocation.distanceSquaredTo(bulletLocation)) / (bullet.speed); // want inverse of cost for now
        return cost;
    }

    static void updateFromInbox() {
        MessageReader.updateInbox();
        for(SpottedItem item : MessageReader.Inbox.items){
            if(item instanceof SpottedRobot){
                arena.updateRobot((SpottedRobot) item);
            }
            else if(item instanceof SpottedTree){
                arena.updateTree((SpottedTree) item);
            }
        }
        for(SpottedItem item : arena.getRobots()){
            System.out.println("I see " + item.toString());
        }
    }

    static void sense() {
        // sensing code
        RobotInfo[] robots = rc.senseNearbyRobots(); // 100 bytecode
        for(RobotInfo robot : robots){
            SpottedRobot srobot = new SpottedRobot(rc.getRoundNum(), robot);
            if(!arena.isUpdated(srobot, rc.getRoundNum())) {
                // if not checked, update internal map
                arena.updateRobot(srobot);
                // and broadcast
                //System.out.println("Writing " + srobot.spottedInformation.ID);
                MessageReader.write(srobot);
            }
            else{
                //System.out.println("Already see " + srobot.spottedInformation.ID);
            }
        }
        /*
        BulletInfo[] bullets = rc.senseNearbyBullets(); // 100 bytecode
        for(BulletInfo bullet : bullets){
            SpottedBullet sbullet = new SpottedBullet(rc.getRoundNum(), bullet);
            if(!arena.isUpdated(sbullet, rc.getRoundNum())) {
                // see if already checked
                arena.updateBullet(sbullet);

            }
        }
        */

        TreeInfo[] trees = rc.senseNearbyTrees(); // 100 bytecode
        for(TreeInfo tree : trees){
            boolean containsRobot = (tree.containedRobot != null);
            SpottedTree stree = new SpottedTree(rc.getRoundNum(), tree, containsRobot);
            if(!arena.isUpdated(stree, rc.getRoundNum())) {
                // see if already checked
                arena.updateTree(stree);
            }
        }
    }

    static void runTank() {
        runCommon();
    }

    static void runScout() {
        runCommon();
    }

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
            runCommon();
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // detect arkan
                    boolean found = false;
                    for(RobotInfo r : robots){
                        if(r.type == RobotType.ARCHON){
                            found = true;
                            final Direction directionToEnemy = rc.getLocation().directionTo(r.location);
                            if(rc.canFirePentadShot()) {
                                rc.firePentadShot(directionToEnemy);
                            }
                            // move
                            if(rc.canMove(directionToEnemy)){
                                rc.move(directionToEnemy);
                            }
                            break;
                        }
                    }

                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                else if (!rc.hasMoved()) {
                    // Move randomly
                    tryMove(randomDirection());
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {
            runCommon();
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.getRoundNum() < 5 && rc.canHireGardener(dir)){
                    rc.hireGardener(dir);
                }
                if (rc.canHireGardener(dir) && Math.random() < .01 && rc.getRoundNum() > 50) {
                    rc.hireGardener(dir);
                }
                if(!rc.hasMoved()) {
                    // Move randomly
                    tryMove(randomDirection());
                }
                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
    

    static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");
        int turnsAlive = 0;

        // The code you want your robot to perform every round should be in this loop
        while (true) {
            runCommon();
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                turnsAlive++;



                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a soldier in this direction
                if (turnsAlive < 3 || turnsAlive > 40){
                    if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                        rc.buildRobot(RobotType.SOLDIER, dir);
                    }
                }

                if(!rc.hasMoved()) {
                    // Move randomly
                    tryMove(randomDirection());
                }
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }


    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
            runCommon();
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // Move Randomly
                        tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of strideLength
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of strideLength
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is  our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= BODY_RADIUS);
    }
}

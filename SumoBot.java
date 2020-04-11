import hlt.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SumoBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Jared");

        // We now have 1 full minute to analyse the initial map.
        final String initialMapIntelligence =
                "width: " + gameMap.getWidth() +
                "; height: " + gameMap.getHeight() +
                "; players: " + gameMap.getAllPlayers().size() +
                "; planets: " + gameMap.getAllPlanets().size();
        Log.log(initialMapIntelligence);

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);

            List<Ship> allMyShips = new ArrayList<Ship>(gameMap.getMyPlayer().getShips().values());
            List<Ship> allEnemyShips = new ArrayList<Ship>(gameMap.getAllShips());
            allEnemyShips.removeIf(s -> (s.getOwner() == gameMap.getMyPlayerId()));
            
            for (int i = 0; i < allMyShips.size(); i++) {
                final Ship ship = allMyShips.get(i);

                if (i > (allMyShips.size() / 4)) {
                    // get planets
                    if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                        continue;
                    }

                    List<Planet> allPlanets = new ArrayList<>(gameMap.getAllPlanets().values());
                    allPlanets.sort(new Comparator<Planet>() {
                        @Override
                        public int compare(Planet p1, Planet p2) {
                            return (int) (p1.getDistanceTo(new Position(ship.getXPos(), ship.getYPos())) - p2.getDistanceTo(new Position(ship.getXPos(), ship.getYPos())));
                        }
                    });

                    for (final Planet planet : allPlanets) {
                        if (planet.isOwned() && (planet.getOwner() == gameMap.getMyPlayer().getId())) {
                            continue;                   
                        }

                        if (ship.canDock(planet)) {
                            moveList.add(new DockMove(ship, planet));
                            break;
                        }

                        final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED/2);
                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        }

                        break;
                    }
                } else {
                    // attack other ships

                    // go to closest ships first
                    allEnemyShips.sort(new Comparator<Ship>() {
                        @Override
                        public int compare(Ship s1, Ship s2) {
                            return (int) (s1.getDistanceTo(new Position(ship.getXPos(), ship.getYPos())) - s2.getDistanceTo(new Position(ship.getXPos(), ship.getYPos())));
                        }
                    });

                    for (final Ship sh:  allEnemyShips) {
                        final ThrustMove thrustTowardsEnemyShip = Navigation.navigateShipTowardsTarget(gameMap, ship, new Position(sh.getXPos(), sh.getYPos()), Constants.MAX_SPEED / 2, true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0);
                        if (thrustTowardsEnemyShip != null) {
                            moveList.add(thrustTowardsEnemyShip);
                        }

                        break;
                    }
                }
            }
            
            Networking.sendMoves(moveList);
        }
    }
}

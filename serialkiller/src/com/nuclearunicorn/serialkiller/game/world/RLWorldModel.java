package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.events.CriminalActionEvent;
import com.nuclearunicorn.serialkiller.game.events.NPCWitnessCrimeEvent;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLActor;
import com.nuclearunicorn.serialkiller.generators.Apartment;
import org.lwjgl.util.Point;
import rlforj.los.ILosBoard;

import java.util.ArrayList;
import java.util.List;

public class RLWorldModel extends WorldModel implements ILosBoard {
    
    private List<RLTile> fovTiles = new ArrayList<RLTile>();
    
    public static Point playerSafeHouseLocation;


    List<Apartment> apartments = new ArrayList<Apartment>(16);

    public RLWorldModel(int layersCount) {
        this.LAYER_COUNT = layersCount;

        for (int i = 0; i< LAYER_COUNT; i++ ){
            WorldLayer layer = new RLWorldLayer();
            layer.set_zindex(i);
            layer.setModel(this);
            worldLayers.put(i, layer);
        }
    }

    /**
        Reset fov-checked tiles.
        Since iterating them all can be heavy operation,
        we will store fov-visited tiles and reset them directly from this list
     */
    public void resetFov(){
        for(RLTile tile: fovTiles){
            if (tile != null){  //FTW?
                tile.setVisible(false);
                tile.setExplored(true);
            }
        }
        fovTiles.clear();
    }


    /*
        Whether we can check FOV radius or not
        Usually we shuld allways return true, except
        corner cases when dynamic chunk loading is prohibited

        TODO: further investigation
     */
    @Override
    public boolean contains(int x, int y) {
        return true;
    }

    @Override
    public boolean isObstacle(int x, int y) {
        RLTile tile = getRLTile(x, y);

        //do not allow to check FOV outside of the cluster
        if (tile == null){
            return true;
        }

        if (tile.isWall()){
            return true;
        }
        if (tile.isBlocked()){
            Entity ent = tile.getEntity();
            if (ent instanceof EntityRLActor){
                if (!((EntityRLActor) ent).isBlockSight()){
                    return false;
                }
            }
            return true;
        }

        return false;
        //return tile.isWall() || tile.isBlocked();
        //return tile.get_height() > 0;
    }

    @Override
    public void visit(int x, int y) {
        RLTile visibleTile = getRLTile(x,y);
        if (visibleTile != null){
            fovTiles.add(visibleTile);
            visibleTile.setVisible(true);
        }
    }
    
    private RLTile getRLTile(int x, int y){
        
        WorldLayer layer = this.getLayer(Player.get_zindex());
        WorldTile tile = layer.get_tile(x,y);

        if(tile instanceof RLTile){
            return (RLTile)tile;
        }
        return null;
    }

    @Override
    public void e_on_event(Event event) {
        super.e_on_event(event);

        if (event instanceof CriminalActionEvent){

            CriminalActionEvent criminalEvent = (CriminalActionEvent)event;

            Entity[] entList = getEnvironment().getEntityManager().getList(Player.get_zindex()).toArray(new Entity[0]);
            for (Entity ent : entList){
                if (ent instanceof EntityRLActor){

                    //TODO: replace it with bresinhem line from npc to player
                    /*if ( ((RLTile)ent.tile).isVisible() &&
                            ((RLTile)this.getLayer(Player.get_zindex()).get_tile(criminalEvent.origin)).isVisible()) {*/

                    if ( ((RLTile)ent.tile).isVisible() && criminalEvent.criminal.isPlayerEnt()){
                        System.out.println( ent.getName() + " is witnessing crime of " + criminalEvent.criminal.getName()+ "@"+criminalEvent.origin);
                        NPCWitnessCrimeEvent witnessCrimeEvent = new NPCWitnessCrimeEvent(criminalEvent.origin, criminalEvent.criminal);
                        ((EntityRLActor) ent).e_on_event(witnessCrimeEvent);
                    }
                }
            }
        }
    }

    public List<Apartment> getApartments() {
        return apartments;
    }

}

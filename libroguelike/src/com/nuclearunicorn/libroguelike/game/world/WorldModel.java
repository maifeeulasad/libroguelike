/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import com.nuclearunicorn.libroguelike.events.EEntityChangeChunk;
import com.nuclearunicorn.libroguelike.events.EEntitySpawn;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EEntityMove;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.utils.LazyLoadWorldElement;
import org.lwjgl.util.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 *
 * @author Administrator
 */
public class WorldModel implements IEventListener {
    final static Logger logger = LoggerFactory.getLogger(WorldModel.class);

    protected GameEnvironment environment = null;

    public static int LAYER_COUNT;    //max depth of geometry layers
    protected final java.util.HashMap<Integer, WorldLayer> worldLayers
            = new java.util.HashMap<Integer, WorldLayer>(LAYER_COUNT);


    private String name;
    /**
    * Load region data from server including village assigment, ownership and so on
    *
    * origin - location of region block, e.g. [-1,1]
    */
    public static LazyLoadWorldElement<WorldRegion> worldRegions =
    new LazyLoadWorldElement<WorldRegion>(){
        @Override
        protected WorldRegion precache(Point origin) {

                WorldRegion region = new WorldRegion();
                region.origin.setLocation(origin);

                //TODO: load region from some external storage
                //(i.e. use server API)
                region.load_data();
                return region;
        }
    };

    public WorldModel(){
        this(10);
    }

    //todo: replace with world builder object
    public WorldModel( int layerCount ){

        this.LAYER_COUNT = layerCount;
        /*
         * Create placeholder for every possile layers, starting 0 as a ground layer,
         * and ending LAYER_COUNT as a deapest underground layer
         *
         */
        for (int i = 0; i< LAYER_COUNT; i++ ){
            WorldLayer layer = null;
            /*if (i == WorldLayer.GROUND_LAYER){
                layer = new WorldLayer();
            }else{
                layer = new UndergroundLayer();
            }*/
            //TODO: request class component LayerFactory
            layer = new WorldLayer();
            layer.set_zindex(i);
            layer.setModel(this);
            worldLayers.put(i, layer);
        }
    }

    public WorldLayer getWorldLayer(int layer_id){
        return worldLayers.get(layer_id);
    }




    //----------------------------EVENTS SHIT-----------------------------------
    public void e_on_event(Event event){
       if (event instanceof EEntityMove){
           EEntityMove move_event = (EEntityMove)event;

           //TODO: fix me
           getWorldLayer(move_event.entity.getLayerId()).
                   move_entity(move_event.entity, move_event.getTo());

           if (move_event.entity.isPlayerEnt()){
               WorldViewCamera.target.setLocation(move_event.entity.origin);
           }
       }
       else if(event instanceof EEntitySpawn){
           EEntitySpawn spawn_event = (EEntitySpawn)event;

           //-------------------------------------------------------------------
           Point chunkOrigin = WorldChunk.get_chunk_coord(spawn_event.ent.origin);
           //System.out.println("checking if ent "+spawn_event.ent.getClass().getName() + " is player ent: " + spawn_event.ent.isPlayerEnt());

           if (spawn_event.ent.isPlayerEnt()){
               WorldCluster.locate(spawn_event.ent.origin);
           }
           WorldChunk new_chunk = getLayer().get_cached_chunk(chunkOrigin, false); //no OutOfBounds check

           if (new_chunk == null ){
               throw new RuntimeException("Failed to get cached chunk for origin @" + chunkOrigin);
           }

           EEntityChangeChunk e_change_chunk = new EEntityChangeChunk(spawn_event.ent, null, new_chunk);
           e_change_chunk.setManager(environment.getEventManager());
           e_change_chunk.post();

           Point entOrigin = spawn_event.ent.origin;
           WorldTile spawn_tile = spawn_event.ent.getLayer().get_tile(entOrigin.getX(), entOrigin.getY());

           /* Some ents are prohabited from spawning. Normaly, that should be checked at the server side
            *
            * For now we will check it at client side (totems for example)
            */
           Point region_coord = WorldRegion.get_region_coord(entOrigin);
           //TODO: get region instance and check if totem can be build there

           if (spawn_tile != null){
                spawn_tile.add_entity(spawn_event.ent);
           }else{

               //show some debug info to hint me next time when this fucking world model crashs again

               if (!environment.getEventManager().hasListener(this)){
                   logger.error("World model is not subscribed to the event manager");
               }

               WorldLayer layer = worldLayers.get(spawn_event.ent.getLayerId());

               logger.error("Tile data size: {}", layer.getTileData(entOrigin, false).size());
               logger.error("Chunk data size: {}", layer.chunk_data.size());

               throw new RuntimeException("Failed to assign spawned entity " + spawn_event.ent.getName() +" to tile@"+entOrigin+"["+getLayer().get_zindex()+"] - tile is null!");

           }

           if (spawn_event.ent.light_amt > 0.0f){
                getLayer().invalidate_light();
           }


           //spawn_event.ent.origin = spawn_event.origin;
           //-------------------------------------------------------------------
       }else if(event instanceof EEntityChangeChunk){

           EEntityChangeChunk e_change_chunk = (EEntityChangeChunk)event;
           Entity ent = e_change_chunk.ent;
           //ent.set_chunk(e_change_chunk.to);

           e_change_chunk.to.add_entity(ent);

           if (ent.isPlayerEnt()){
                getLayer().update_terrain();

                WorldCluster.locate(e_change_chunk.to.origin);
                getLayer().chunk_gc();
           }

       }
       /*
       else if(event instanceof ETakeDamage){
           //drop blood if someone is taking damage
           ETakeDamage dmg_event = (ETakeDamage)event;
           if (dmg_event.dmg.type != Damage.DamageType.DMG_FIRE &&
               dmg_event.dmg.type != Damage.DamageType.DMG_MAGIC ){
               Point dmg_origin = new Point();
               dmg_origin.setLocation(dmg_event.ent.origin);

               WorldTile tile = getLayer().get_tile(dmg_origin.getX(), dmg_origin.getY());
               if (!tile.has_ent(EntDecalBlood.class)){
                   EntDecalBlood blood = new EntDecalBlood();
                   blood.setLayerId(dmg_event.ent.getLayerId());
                   blood.spawn(dmg_origin);

                   //TODO: set random dx, dy for more natural blood drops?
               }else{
                   EntDecalBlood blood = (EntDecalBlood)tile.getEntity(EntDecalBlood.class);
                   blood.nextTile();
               }
           }

       }*/

    }
    //--------------------------------------------------------------------------
    public void e_on_event_rollback(Event event){
       if (event instanceof EEntityMove){
           EEntityMove move_event = (EEntityMove)event;
           getLayer().move_entity(move_event.entity, move_event.getFrom());
       }
    }

    //TODO:
    //link layer to the entity layer
    private WorldLayer getLayer() {
        return worldLayers.get(WorldLayer.GROUND_LAYER);
    }

    //--------------------------------------------------------------------------

    public void update(){
        //1. update timer data
        //2. check if think call is allowed
        //3. call think

        WorldTimer.tick();

        for(WorldLayer layer: worldLayers.values()){
            layer.update();
        }
    }

    public void setEnvironment(GameEnvironment environment) {
        this.environment = environment;
        environment.getEventManager().subscribe(this);
    }
    
    public GameEnvironment getEnvironment(){
        return environment;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection<WorldLayer> getLayers() {
        return worldLayers.values();
    }
    
    public WorldLayer getLayer(int zindex){
        return worldLayers.get(zindex);
    }

    public void reset() {
        //worldLayers.clear();
        for(WorldLayer layer: worldLayers.values()){
            layer.reset();
        }
        worldRegions.clear();
    }
}

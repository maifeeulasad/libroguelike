/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.client.clientIo.gameclient;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.network.EPlayerLogon;
import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.ent.buildings.BuildManager;
import com.nuclearunicorn.libroguelike.game.ent.buildings.EntBuilding;
import com.nuclearunicorn.libroguelike.game.player.Player;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.lwjgl.util.Point;

/**
 *
 * @author bloodrizer
 */
public class GameServClientHandler extends SimpleChannelHandler {
    
    final ClientBootstrap bootstrap;

    public GameServClientHandler(ClientBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        e.getChannel().close();

        throw new RuntimeException("Game server connection: unexpected exception", e.getCause());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        String request = (String) e.getMessage();
        System.err.println("char client: recieved msg: ["+request+"]");

        String[] packet = null;
        if (request != null){
            packet = request.split(" ");
            handlePacket(packet, ctx.getChannel());
        }
    }

     private void handlePacket(String[] packet, Channel ioChannel) {
        //throw new UnsupportedOperationException("Not yet implemented");
        if (packet.length == 0){
            return;
        }
        String eventType = packet[0];

        if (eventType.equals("EPlayerSpawn")){
            int x = Integer.parseInt(packet[1]);
            int y = Integer.parseInt(packet[2]);


            EPlayerLogon event = new EPlayerLogon(new Point(x,y));
            ClientEventManager.addEvent(event);
            //event.post();
        }

        if (eventType.equals("EEntitySpawn")){
            int spawnType = Integer.parseInt(packet[1]);

            String entType = packet[2];
            int x = Integer.parseInt(packet[3]);
            int y = Integer.parseInt(packet[4]);

            switch (spawnType) {

                case 0:
                    Class building = BuildManager.get_building(entType);
                    EntBuilding ent_building;
                    try {
                        ent_building = (EntBuilding) building.newInstance();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                    ent_building.set_combat(new BasicCombat());
                    ent_building.setLayerId(Player.get_zindex()); //TODO: get layer_id from server
                    ent_building.setEnvironment(ClientGameEnvironment.getEnvironment());

                    ent_building.spawn("54321" //<<<<<< UID THERE!!!!!!!!!!
                        , new Point(x,y));
                break;

            }

        }
    }

}
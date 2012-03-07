package com.nuclearunicorn.serialkiller.game.world.entity;

import com.nuclearunicorn.libroguelike.game.ent.EntityNPC;
import com.nuclearunicorn.serialkiller.render.MessageRenderer;

/**
 * Created by IntelliJ IDEA.
 * User: dpopov
 * Date: 07.03.12
 * Time: 15:10
 * To change this template use File | Settings | File Templates.
 */
public class EnityRLHuman extends EntityNPC {

    enum Sex {
        MALE,
        FEMALE
    }

    enum Race {
        WHITE,
        BLACK,
        ASIAN
    }

    enum Religion {
        ATHEIST
    }

    Sex sex = Sex.MALE;
    int age = 30;
    Race race = Race.WHITE;
    Religion religion = Religion.ATHEIST;

    //TODO: apartment link
    
    public void describe(MessageRenderer render){
        render.message(name + " is " + sex + ", age " + age);

        //etc etc
    }
    
    public String getModel(){
        return "@";
    }
}

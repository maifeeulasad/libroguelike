package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.ActionList;
import com.nuclearunicorn.libroguelike.game.ent.BaseEntityAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.EquipContainer;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.bodysim.Limb;
import com.nuclearunicorn.serialkiller.game.events.ShowDetailedInformationEvent;
import com.nuclearunicorn.serialkiller.game.personality.BasePersonality;
import com.nuclearunicorn.serialkiller.game.social.CrimeRecord;
import com.nuclearunicorn.serialkiller.generators.Apartment;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

import java.util.*;

/**
 */
public class EntityRLHuman extends EntityRLActor {
    public enum Sex {
        MALE,
        FEMALE
    }

    public enum Race {

        WHITE("white"),
        BLACK("black"),
        ASIAN("asian"),
        HISPANIC("hispanic");

        String displayName;

        private static final List<Race> VALUES =
                Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();

        Race(String name) {
            this.displayName = name;
        }

        public String diplayName(){
            return displayName;
        }

        public static Race getRandomRace()  {
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }

    public enum Religion {
        ATHEIST("not religious"),
        CHRISTIAN("christian"),
        JEWISH("jewish"),
        MUSLIM("muslim");

        String displayName;

        private static final List<Religion> VALUES =
                Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();

        Religion(String name) {
            this.displayName = name;
        }

        public String diplayName(){
            return displayName;
        }

        public static Religion getRandomReligion()  {
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }

    Sex sex = Sex.MALE;
    public int age = 30;
    public Race race = Race.WHITE;
    public Religion religion = Religion.ATHEIST;

    BodySimulation bodysim;
    BasePersonality personality;
    public List<CrimeRecord> crimeRecords = new ArrayList<CrimeRecord>();

    //social stuff like family
    EntityRLHuman mate;
    EntityRLHuman parent;
    List<EntityRLHuman> children = new ArrayList<EntityRLHuman>();
    List<EntityRLHuman> siblings = new ArrayList<EntityRLHuman>();

    public EquipContainer equipment = new EquipContainer();

    //TODO: apartment link
    Apartment apartment;

    public EntityRLHuman(){
        super();

        setBodysim(new BodySimulation());
        setPersonality(new BasePersonality());
    }

    private void setPersonality(BasePersonality personality) {
        this.personality = personality;
    }

    public Entity getParent() {
        return parent;
    }

    public boolean isAdult() {
        return age >= 18;
    }

    public void setMate(EntityRLHuman mate){
        if (this.mate == null){ //inf cycle safe switch
            this.mate = mate;
            mate.setMate(this);
        }
    }

    public void addChild(EntityRLHuman child) {
        for(EntityRLHuman otherChild:children){
            otherChild.addSibling(child);
        }

        children.add(child);
        child.setParent(this);
    }

    private void setParent(EntityRLHuman parent) {
        this.parent = parent;
    }

    private void addSibling(EntityRLHuman child) {
        if (!siblings.contains(child)){
            siblings.add(child);
            child.addSibling(this);
        }
    }

    public EntityRLHuman getMate(){
        return mate;
    }

    public void addCrimeRecord(CrimeRecord crimeRecord) {
        for (CrimeRecord crime: crimeRecords){
            if (crime.crimeType == crimeRecord.crimeType){
                crime.incCount();
                return;
            }
        }
        crimeRecords.add(crimeRecord);
    }

    public void setSex(Sex sex){
        this.sex = sex;
    }

    public Sex getSex(){
        return sex;
    }

    public Apartment getApartment(){
        return apartment;
    }

    public void setApartment(Apartment apt){
        apartment = apt;
    }
    
    public String getModel(){
        return "@";
    }

    /*
        Since this game is turn-based, entity is allways awake

        TODO: fix possible bug with awakeness
     */
    @Override
    public boolean is_awake(long current_time_ms){
        return true;
    }

    @Override
    public void think(){
        bodysim.think();

        if (!bodysim.isStunned()){
            super.think();
        }

        updateASCIIModel();
    }

    @Override
    public void update() {
        if (!bodysim.isStunned()){
            if (controller != null){
                controller.think();
            }

            //ai also disabled if stunned. (not very good)
            if (ai != null){
                ai.update();
            }
        }

    }

    private void updateASCIIModel() {
        AsciiEntRenderer renderer = (AsciiEntRenderer)get_render();
        renderer.symbol = "@";

        if (ai != null){
            //TODO: move into internal method
            if (ai.getState() == PedestrianAI.AI_STATE_ESCAPING){
                renderer.symbol = "!";
            }
            if (ai.getState() == PedestrianAI.AI_STATE_CHASING){
                renderer.symbol = "!";
            }
            if (ai.getState() == PedestrianAI.AI_STATE_SLEEPING){
                renderer.symbol = "Z";
            }
        }
        if (bodysim.isStunned()){
            renderer.symbol = "?";
        }
        if (combat != null && !combat.is_alive()){
            renderer.symbol = "%";
        }
    }

    @Override
    public void die(Entity killer){
        super.die(killer);

        ((AsciiEntRenderer)this.render).symbol = "%";
        RLMessages.message(name + " has died", Color.red);

        set_blocking(false);
    }

    @Override
    public void describe(){
        super.describe();

        RLMessages.message(name + " is " + sex + ", age " + age, Color.lightGray);
        RLMessages.message(name + " is " + race + ", " + religion, Color.lightGray);

        if (!crimeRecords.isEmpty()){
            RLMessages.message(name + " is criminal", new Color(250,160,160));
        }

        //if (this.apt)

        int maxHp;
        if (combat != null){
            maxHp = combat.get_max_hp();

            if (combat.get_hp() >= maxHp){
                RLMessages.message(name + " is healthy", Color.lightGray);
            } else if (combat.get_hp() >= maxHp * 0.5){
                RLMessages.message(name + " is injured", Color.lightGray);
            } else if (combat.get_hp() > 0 ){
                RLMessages.message(name + " is near death", Color.lightGray);
            } else if (combat.get_hp() <= 0 ){
                RLMessages.message(name + " is dead", Color.lightGray);
            }
        }


    }

    public BodySimulation getBodysim() {
        return bodysim;
    }

    public void setBodysim(BodySimulation bodysim) {
        this.bodysim = bodysim;
        bodysim.setOwner(this);
    }

    @Override
    public void e_on_event(Event event) {
        if (this.ai != null){
            ai.e_on_event(event);
        }
    }

    @Override
    public void kill(EntityRLActor victim) {
        if (!(victim instanceof EntityRLHuman)){
            return;
        }

        if (this.bodysim != null){
            float bloodlustAmt = 20f;

            if (this.personality != null){
                personality.registerVictim((EntityRLHuman) victim);
                bloodlustAmt = personality.getVictimModifier((EntityRLHuman) victim);

                if (bloodlustAmt <= 5f){
                    bloodlustAmt = 5f;
                }
            }
            System.err.println("lowering bloodlust by "+bloodlustAmt);
            this.bodysim.depleteBloodlust(bloodlustAmt);
        }
    }

    @Override
    public ArrayList get_action_list() {
        class ActionDetailedInformation extends BaseEntityAction {

            @Override
            public void execute() {
                ((IEventListener)Game.get_game_mode().get_ui()).e_on_event(new ShowDetailedInformationEvent(owner));
            }

        }

        class ActionDismember extends BaseEntityAction {

            @Override
            public void execute() {
                List<Limb> limbs = ((EntityRLHuman)owner).bodysim.getLimbs();
                if (limbs.isEmpty()){
                    return; //no more limbs to cut
                }
                Limb limb = limbs.get((int)(Math.random()*limbs.size()));
                limbs.remove(limb);

                BaseItem limbItem = BaseItem.produce(owner.getName() + "'s "+limb.getName(),1);
                limbItem.setEffect("damage","1");
                limbItem.setEffect("damage_type","dmg_blunt");
                limbItem.setEffect("restore_hunger","20");
                limbItem.setEffect("restore_hp","2");

                if (((EntityRLHuman)owner).bodysim.isInfected()){
                    limbItem.setEffect("infected","true");
                }

                limbItem.set_slot("weapon");
                //todo: replace with action caller
                ((EntityRLHuman)Player.get_ent()).getContainer().add_item(limbItem);

                RLMessages.message( Player.get_ent().getName() + " cuts off " + owner.getName() + "'s "+limb.getName(), Color.orange );

                /*if self.owner.is_alive():
                self.owner.em.services["render"].message(self.owner.name+" screams in agony");
                #limb loss should ALLWAYS result in blodloss, so - dmg_cut
                self.owner.take_damage(bodysim.Damage(20*limb.dmg_multiply,"dmg_cut",caller))*/
            }
        }

        class ActionRape extends BaseEntityAction {
            @Override
            public void execute() {
                BodySimulation bodysim =((EntityRLHuman)owner).bodysim;
                if (bodysim.getAttribute("libido") < 20f){
                    RLMessages.message("You are not in the mood", Color.orange);
                    return;
                }
                bodysim.setAttribute("libido", 0f);

                bodysim.depleteBloodlust(10f);

                RLMessages.message( Player.get_ent().getName() + " rapes " + owner.getName(), Color.orange );
                if (bodysim.isInfected()){
                    //~50% chance to catch STD
                    if (new Random().nextInt(100) <= 50){
                        ((EntityRLHuman)Player.get_ent()).bodysim.setInfected(true);
                    }
                }
            }

        }

        boolean isAlive = true;
        boolean isStunned = false;
        if (combat != null && !combat.is_alive()){
            isAlive = false;
        }
        if (bodysim != null && bodysim.isStunned()){
            isStunned = true;
        }

        ActionList<Entity> list = new ActionList();
        list.set_owner(this);
        list.add_action(new ActionDetailedInformation(),"Detailed info");
        if (!isAlive){
            list.add_action(new ActionDismember(),"Dismember");
        }
        if (!isAlive || isStunned){
            EntityRLHuman playerEnt = ((EntityRLHuman)Player.get_ent());
            if (playerEnt.getSex().equals(Sex.MALE) && playerEnt.isAdult()){
                list.add_action(new ActionRape(),"Rape");
            }
        }

        return list.get_action_list();
    }
}

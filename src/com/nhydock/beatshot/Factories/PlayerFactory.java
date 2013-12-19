package com.nhydock.beatshot.Factories;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.nhydock.beatshot.CEF.Components.Ammo;
import com.nhydock.beatshot.CEF.Components.Bound;
import com.nhydock.beatshot.CEF.Components.Health;
import com.nhydock.beatshot.CEF.Groups.Player;
import com.badlogic.gdx.artemis.components.*;
import com.nhydock.beatshot.core.Consts.DataDir;
import com.nhydock.beatshot.core.Consts.PlayerInput;
import com.nhydock.beatshot.logic.Bullet.BulletEmitter;
import com.nhydock.beatshot.util.SpriteSheet;

/**
 * 
 * @author nhydock
 *
 */
public class PlayerFactory {

	public static final int MAXHP = 100;
	public static final int MAXAMMO = 100;
	
	private static SpriteSheet sprite;
	private static Texture shadow_sprite;
	
	static
	{
		sprite = new SpriteSheet(Gdx.files.internal(DataDir.Images + "player.png"), 4, 1);
		shadow_sprite = new Texture(Gdx.files.internal(DataDir.Images + "player_shadow.png"));
	}

	/**
	 * Formats an entity into a player
	 * @param e - entity to convert
	 * @return a formatted player entity
	 */
	public static Entity createEntity(Entity e) {
		World w = e.getWorld();
		
		e.addComponent(new Health(MAXHP), Health.CType);
		e.addComponent(new Ammo(MAXAMMO), Ammo.CType);
		
		e.addComponent(new Position(0, 0, -sprite.getFrameWidth()/2f, 5), Position.CType);
		e.addComponent(new Velocity(0, 0), Velocity.CType);
		
		e.addComponent(new Bound(10f, 10f), Bound.CType);
		
		e.addComponent(new Renderable(sprite.getFrame(0)), Renderable.CType);
		e.addComponent(new Animation(sprite.getTexture(), sprite.frameCount, .1667f, true), Animation.CType);
		e.addComponent(new Player(), Player.CType);
		e.addComponent(new InputHandler(new int[]{Input.Keys.LEFT, Input.Keys.RIGHT}), InputHandler.CType);
		
		float x = -5;
		for (int i = 0, angle = 50; i < PlayerInput.Lasers; i++, x += 5f, angle -= 25)
		{
			Entity laser = e.getWorld().createEntity();
			laser = BulletEmitter.createEmitter(laser, 10, .1f, e);
			Position p = (Position) laser.getComponent(Position.CType);
			p.offset.x = x;
			p.offset.y = sprite.getFrameHeight();
			Velocity v = (Velocity) laser.getComponent(Velocity.CType);
			v.setTo(angle, 180f);
			laser.addComponent(new InputHandler(PlayerInput.values()[i].keys), InputHandler.CType);
			laser.addComponent(new Player(), Player.CType);
			laser.addToWorld();
		}
		
		Entity shadow = e.getWorld().createEntity();
		shadow.addComponent(new Position(0,0,0,-2), Position.CType);
		shadow.addComponent(new Anchor(e), Anchor.CType);
		shadow.addComponent(new Renderable(new Sprite(shadow_sprite)), Renderable.CType);
		e.getWorld().getManager(TagManager.class).register("PlayerShadow", shadow);
		shadow.addToWorld();
		
		return e;
	}

}
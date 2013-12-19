package com.nhydock.beatshot.logic.level;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.artemis.components.*;
import com.badlogic.gdx.artemis.systems.*;
import com.nhydock.beatshot.CEF.*;
import com.nhydock.beatshot.CEF.Components.*;
import com.nhydock.beatshot.logic.level.LevelData.Background.*;
import com.nhydock.beatshot.logic.level.LevelData.*;

import com.nhydock.beatshot.Factories.PlayerFactory;
import com.nhydock.beatshot.core.Consts.DataDir;
import com.nhydock.beatshot.logic.Engine;
import com.nhydock.beatshot.logic.Bullet.BulletEmitter;

import com.nhydock.beatshot.util.SpriteSheet;

public class Level{
	
	/**
	 * Defines how we handle enemy spawning
	 * In endurance mode, enemy spawns are randomly selected and just occur every 1000 meters
	 * You also get points for how far you progress in endurance mode
	 * 
	 * In story mode, enemys spawn where they are laid out in the level and are capable of blocking progression until defeated
	 * Score bonuses are recieved for beating a level quickly and for defeating all enemies
	 */
	private static boolean EnduranceMode = true;
	
	/**
	 * How often spawns should occur in Endurance Mode
	 */
	private static float ENDURANCERATE = 100f;
	
	private int currentEnemyGroup;
	
	//game load data files
	public LevelData data;
		
	public static final float[] FOV = {50, 75, 190, 220};

	/**
	 * Container of all the entities for this level
	 */
	public World world;
	
	private float distance;
	private float totalDistance;
	
	private Array<Entity> activeEnemies;
	private Array<Entity> oldEnemies;
	
	RenderSystem rs;
	EnemySystem es;

	private Sound warningBeep;

	private float warning;
	
	private static final float WARNING_WAIT = 2.0f;

	/**
	 * Loads and constructs a level
	 * @param id - name of the level
	 */
	public Level(String id)
	{
		data = new LevelData(id);
		world = new World();
		
		world.setManager(new TagManager());
		world.setManager(new GroupManager());
		
		world.setSystem(new AnimationSystem());
		world.setSystem(new BulletLifeSystem());
		world.setSystem(new EmitterSystem());
		world.setSystem(new CollisionEntitySystem());
		world.setSystem(new MovementSystem());
		
		world.setSystem(new PlayerInputSystem());
		
		rs = new RenderSystem();
		es = new EnemySystem();
		world.setSystem(rs, true);
		world.setSystem(es, true);
		
		Entity player = PlayerFactory.createEntity(world.createEntity());
		world.getManager(TagManager.class).register("Player", player);
		
		player.addToWorld();
		
		Engine.world = world;
		Engine.player = player;
		
		activeEnemies = new Array<Entity>();
		oldEnemies = new Array<Entity>();
	}
	
	/**
	 * Updates game logic
	 * @param delta
	 */
	public void advance(float delta)
	{
		if (Engine.GameOver)
		{
			world.setDelta(0);
			return;
		}
		
		if (warning >= 0f)
		{
			warning -= delta;
		}
		else
		{
			world.getSystem(RenderSystem.class).warning = false;
		}
		
		world.setDelta(delta);
		world.process();
		
		float travel = 10f*delta;
		if (EnduranceMode)
		{
			distance += travel;
			Engine.score += travel;
			if (distance > ENDURANCERATE)
			{
				totalDistance += distance;
				distance -= ENDURANCERATE;
				es.killEnemies();
				SpawnSet enemyData = data.enemyData.get((int)(Math.random()*data.enemyData.size));
				world.getSystem(RenderSystem.class).warning = (enemyData.warning);
				warning = (enemyData.warning)?2.0f:0;
				activeEnemies = es.spawnEnemies(enemyData);
				//warningBeep.play();
			}
		}
		es.process();
		
		if (Engine.player.getComponent(Health.class).hp <= 0)
		{
			Engine.GameOver = true;
		}
	}
	
	/**
	 * Draws the render system
	 * @param batch
	 */
	public void draw(SpriteBatch batch)
	{
		world.getSystem(RenderSystem.class).draw(batch);
	}

	/**
	 * sets all the level's data into its starting positions
	 */
	public void start()
	{
		GroupManager gm = this.world.getManager(GroupManager.class);
		TagManager tm = this.world.getManager(TagManager.class);
		
		// place the enemies in the world
		for (int i = 0; i < data.enemyData.size; i++)
		{
			SpawnSet s = data.enemyData.get(i);
			for (int j = 0; j < s.spawns.size; j++)
			{
				Spawn enemy = s.spawns.get(j);
				
				Entity e = this.world.createEntity();
				e = s.atlas.createEnemy(enemy.name, e);
				Position p = (Position)e.getComponent(Position.CType);
				p.location.x = enemy.pos.x;
				p.location.y = enemy.pos.y;
				Entity emitter = this.world.createEntity();
				emitter = BulletEmitter.createEmitter(emitter, 10, 1.0f, e);
				
				this.world.addEntity(e);
			}
		}
		
		//create layered background
		for (int i = 0; i < data.background.stack.size; i++)
		{
			
			Entity layer = this.world.createEntity();
			
			FieldData f = data.background.stack.get(i);
			Texture t = Engine.assets.get(f.image, Texture.class);
			Sprite s = new Sprite(t);
			
			if (f instanceof LayerData)
			{
				LayerData d = (LayerData)data.background.stack.get(i);
				t.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
				s.setSize(Math.max(t.getWidth(), FOV[2]), Math.max(t.getHeight(),FOV[3]));
				layer.addComponent(new Position(), Position.CType);
					
				layer.addComponent(new Scrollable(0, d.rate, Math.max(FOV[2]/(float)t.getWidth(), 1.0f), Math.max(FOV[3]/(float)t.getHeight(), 1.0f)), Scrollable.CType);
			}
			else if (f instanceof StaticData)
			{
				StaticData d = (StaticData)data.background.stack.get(i);
				layer.addComponent(new Position(d.x, d.y), Position.CType);
				layer.addComponent(new Angle(0), Angle.CType);
				layer.addComponent(new Rotation(d.dps), Rotation.CType);
			}
			
			layer.addComponent(new Renderable(s), Renderable.CType);
			
			gm.add(layer, "Field");
			this.world.addEntity(layer);
		}
		
		//load banners
		{
			Texture t = Engine.assets.get(DataDir.Ui+"banners.png", Texture.class);
			t.setWrap(TextureWrap.Repeat, TextureWrap.ClampToEdge);
			SpriteSheet bannerTex = new SpriteSheet(t, 1, 3);
			for (int i = 0; i < 3; i++)
			{
				Entity e = this.world.createEntity();
				TextureRegion r = bannerTex.getFrame(i);
				Sprite s = new Sprite(r);
				s.setSize(FOV[3], 12);
				s.setRotation(90);
				s.setOrigin(0, 0);
				
				e.addComponent(new Position(0, 0, 12, 0));
				e.addComponent(new Scrollable(.35f, 0f, FOV[3]/bannerTex.getFrameWidth(), 1f, r));
				e.addComponent(new Renderable(s));
				
				gm.add(e, "Banner");
				gm.add(e, "Banner"+(char)(i+65));
				
				e.addToWorld();
				
				e = this.world.createEntity();
				s = new Sprite(r);
				
				s.setSize(FOV[3], 12);
				s.setRotation(-90);
				s.setOrigin(0, 0);
				s.flip(false, true);
				
				e.addComponent(new Position(FOV[2], 0, -12, FOV[3]));
				e.addComponent(new Scrollable(-.35f, 0f, FOV[3]/bannerTex.getFrameWidth(), 1f, r));
				e.addComponent(new Renderable(s));
				
				gm.add(e, "Banner");
				gm.add(e, "Banner"+(char)(i+65));
				e.addToWorld();
			}
		}
		
		//game over banner
		{
			Entity gameOverBanner;

			gameOverBanner = world.createEntity();
			Sprite s = new Sprite(Engine.assets.get(DataDir.Ui+"gameover.png", Texture.class));
			s.setPosition(0, 80);
			gameOverBanner.addComponent(new Renderable(s));
			tm.register("GameOver", gameOverBanner);
			gameOverBanner.addToWorld();
		}
		
		//warning banner
		{
			Entity warningBanner;

			warningBanner = world.createEntity();
			Sprite s = new Sprite(Engine.assets.get(DataDir.Ui+"enemywarning.png", Texture.class));
			s.setPosition(0, 140);
			warningBanner.addComponent(new Renderable(s));
			tm.register("Warning", warningBanner);
			warningBanner.addToWorld();
		}
		
		Position p = (Position)Engine.player.getComponent(Position.CType);
		p.location.x = FOV[2]/2.0f;
		
		this.world.initialize();
		
		Engine.score = 0f;
		Engine.GameOver = false;
		
		warningBeep = Engine.assets.get(DataDir.SFX + "warning.wav", Sound.class);
		
	}

	/**
	 * Loads the assets necessary for the level to be displayed
	 */
	public void loadAssets() {
		for (int i = 0; i < data.background.stack.size; i++)
		{
			FieldData f = data.background.stack.get(i);
			Engine.assets.load(f.image, Texture.class);
		}
		//Engine.assets.load(data.bgm, Music.class);
		Engine.assets.load(DataDir.Ui + "banners.png", Texture.class);
		
		Engine.assets.load(DataDir.SFX + "warning.wav", Sound.class);
		Engine.assets.load(DataDir.Ui + "gameover.png", Texture.class);
		Engine.assets.load(DataDir.Ui + "enemywarning.png", Texture.class);
	}

	public void unloadAssets() {
		for (int i = 0; i < data.background.stack.size; i++)
		{
			FieldData f = data.background.stack.get(i);
			Engine.assets.unload(f.image);
		}
		//Engine.assets.load(data.bgm, Music.class);
		Engine.assets.unload(DataDir.Ui + "banners.png");
		Engine.assets.unload(DataDir.SFX + "warning.wav");
		Engine.assets.unload(DataDir.Images + "gameover.png");
	}
}
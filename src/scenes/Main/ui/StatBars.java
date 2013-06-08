package scenes.Main.ui;

import scenes.Main.PlayerNotification;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.shipvgdc.sugdk.graphics.SpriteSheet;
import com.shipvgdc.sugdk.util.Observer;

import logic.Consts.DataDir;
import logic.Engine;
import logic.Player;

public class StatBars extends Sprite implements Observer<PlayerNotification>{
	
	private static Texture frameTex;
	private static SpriteSheet tabsTex;
	private static SpriteSheet barsTex;
	
	
	/**
	 * Specify all assets that need to be loaded first before this class may be instantiated
	 */
	public static void loadAssets()
	{
		Engine.assets.load(DataDir.Ui + "barframe.png", Texture.class);
		Engine.assets.load(DataDir.Ui + "tabs.png", Texture.class);
		Engine.assets.load(DataDir.Ui + "statbar.png", Texture.class);
	}
	
	private Bar hpBar;
	private Bar ammoBar;
	
	public StatBars()
	{
		super();
		frameTex = Engine.assets.get(DataDir.Ui + "barframe.png", Texture.class);
		frameTex.setWrap(TextureWrap.ClampToEdge, TextureWrap.Repeat);
		tabsTex = new SpriteSheet(Engine.assets.get(DataDir.Ui + "tabs.png", Texture.class), 4, 1);
		Texture t = Engine.assets.get(DataDir.Ui + "statbar.png", Texture.class);
		t.setWrap(TextureWrap.ClampToEdge, TextureWrap.Repeat);
		barsTex = new SpriteSheet(t, 3, 1);
		hpBar = new Bar(0);
		ammoBar = new Bar(1);
		ammoBar.setX(25);
	}
	
	public void clear()
	{
		frameTex = null;
		tabsTex = null;
		
		Engine.assets.unload(DataDir.Ui + "barframe.png");
		Engine.assets.unload(DataDir.Ui + "tabs.png");
		Engine.assets.unload(DataDir.Ui + "bars/ammo.png");
		Engine.assets.unload(DataDir.Ui + "bars/hp.png");
		Engine.assets.unload(DataDir.Ui + "bars/empty.png");
	}
	
	public void setPosition(float x, float y)
	{
		super.setPosition(x, y);
		hpBar.setPosition(x, y);
		ammoBar.setPosition(x+25, y);
	}
	
	public void draw(SpriteBatch batch, float alpha)
	{
		hpBar.draw(batch, alpha);
		ammoBar.draw(batch, alpha);
	}
	
	public void draw(SpriteBatch batch)
	{
		this.draw(batch, 1.0f);
	}
	
	private static class Bar extends Actor
	{
		TextureRegion bar;
		TextureRegion back;
		TextureRegion frame;
		TextureRegion typetab;
		TextureRegion decotab;
		
		float fill;
		int type;
		
		static final int BARHEIGHT = 210;
		static final int REPEAT = 105;
		
		public Bar(int type)
		{
			super();
			typetab = tabsTex.getFrame(type);
			decotab = tabsTex.getFrame(3);
			
			frame = new TextureRegion(frameTex);
			frame.setV2(224);
			
			bar = barsTex.getFrame(type);
			back = barsTex.getFrame(2);
			back.setV2(REPEAT);
			fill = 1.0f;
		}
		
		public void draw(SpriteBatch batch, float alpha)
		{
			batch.draw(typetab, this.getX(), this.getY()+222);
			batch.draw(frame, this.getX(), this.getY(), frame.getRegionWidth(), 224);
			batch.draw(decotab, this.getX(), this.getY()+224-decotab.getRegionHeight());
			batch.draw(back, this.getX() + 6, this.getY() + 5, 12, BARHEIGHT);
			bar.setV2(REPEAT*fill);
			batch.draw(bar, this.getX() + 6, this.getY() + 5, 12, BARHEIGHT*fill);
		}
		public void setFill(float amount)
		{
			fill = amount;
		}
	}

	@Override
	public void update(PlayerNotification type, Object... values) {
		if (type == PlayerNotification.HPChange)
		{
			int hp = (int)values[0];
			float fill = hp / (float)Player.MAXHP;
			hpBar.setFill(fill);
		}
		else if (type == PlayerNotification.AmmoChange)
		{
			int ammo = (int)values[0];
			float fill = ammo / (float)Player.MAXAMMO;
			ammoBar.setFill(fill);
		}
	}
}
package discreet_fisher;

import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;

import java.awt.*;

@ScriptManifest(author = "Tasemu", info = "Simple Power Fisher", name = "Discreet Fisher", version = 1.0, logo = "")
public class main extends Script {
	private String thread = "https://osbot.org/forum/topic/58775";
	private NPC target;
	private boolean isDropping = false;
	private String fishAction = "Net";
	private String fishingTool = "Small fishing net";
	private Position targetLastPosition;

	@Override
	public void onStart() {
		log("Welcome to " + this.getName() + " v" + this.getVersion());
		log("Report any issues and bugs here: " + this.thread);
		this.getExperienceTracker().start(Skill.FISHING);
	}

	private enum State {
		FISH,
		DROP,
		WAIT
	};

	private State getState() {		
		if (this.isFishing()) {
			return State.WAIT;
		}
		
		if (this.isReadyToDrop()) {
			return State.DROP;
		}
		
		if (this.isReadyToFish()) {
			return State.FISH;
		}
		
		return State.WAIT;
	}

	@Override
	public int onLoop() throws InterruptedException {
		switch (getState()) {
		case DROP:
			log("dropping fish");
			this.getInventory().dropAllExcept(this.fishingTool);
			this.isDropping = true;
			this.target = null;
			
			break;
		case FISH:
			log("fishing");
			this.target = this.getNpcs().closest("Fishing spot");
			this.targetLastPosition = target.getPosition();
			this.isDropping = false;
			
			if (this.target != null) {
				if (this.target.interact(this.fishAction)) {
					new ConditionalSleep(random(1500, 3000)) {
						@Override
						public boolean condition() throws InterruptedException {
							return target != null;
						}
					}.sleep();
				}
			}
			
			break;
		case WAIT:
			
			if (this.getDialogues().isPendingContinuation()) {
				this.getDialogues().clickContinue();
			}
			
			if (this.fishingSpotGone() || this.targetHasMoved()) {
				this.target = null;
			}
			
			if (this.finishedDropping()) {
				this.target = null;
				this.isDropping = false;
			}
			
			break;
		}
		return random(200, 300);
	}
	
	private boolean targetHasMoved() {
		return this.target != null &&
			   this.targetLastPosition != null &&
			   this.targetLastPosition.getX() == this.target.getX() &&
			   this.targetLastPosition.getY() == this.target.getY();
	}
	
	private boolean fishingSpotGone() {
		return this.target != null && !this.target.exists();
	}

	private boolean finishedDropping() {
		return this.isDropping && this.getInventory().isEmptyExcept(this.fishingTool);
	}
	
	private boolean isFishing() {
		return this.myPlayer().isAnimating() && this.target != null;
	}
	
	private boolean isReadyToDrop() {
		return this.getInventory().isFull() && !this.isDropping;
	}
	
	private boolean isReadyToFish() {
		return !this.myPlayer().isAnimating() && !this.isDropping && this.target == null;
	}
	
	@Override
	public void onExit() {
		log("Thanks for running " + this.getName() + "!");
	}

	@Override
	public void onPaint(Graphics2D g) {
		g.setColor(Color.WHITE);
		g.drawString(this.getName() + " v" + this.getVersion(), 10, 25);
		g.drawString("Status: " + this.getState().toString().toLowerCase() + "ing", 10, 40);
		g.drawString("Fishing XP: " + this.getExperienceTracker().getGainedXP(Skill.FISHING), 10, 55);
	}

}

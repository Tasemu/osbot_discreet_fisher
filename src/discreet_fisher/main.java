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
	private String fishAction = "Lure";
	private String fishingTools = "Fly fishing rod";
	private Position targetLastPosition;
	private long startTime;
	private String status;

	@Override
	public void onStart() {
		log("Welcome to " + this.getName() + " v" + this.getVersion());
		log("Report any issues and bugs here: " + this.thread);
		this.getExperienceTracker().start(Skill.FISHING);
		this.startTime = System.currentTimeMillis();
		this.status = "Initializing";
	}

	private enum State {
		FISH,
		DROP,
		WAIT,
		MOVE
	};

	private State getState() {
		if (this.isMovingToFishingSpot()) {
			return State.MOVE;
		}
		
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
		case MOVE:
			this.status = "Moving to fishing spot";
			
			break;
		case DROP:
			this.status = "Dropping fish";
			this.getInventory().dropAllExcept(this.fishingTools, "Feather");
			this.isDropping = true;
			this.target = null;
			
			break;
		case FISH:
			this.status = "Fishing";
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
			this.status = "Waiting...";
			
			if (this.getDialogues().isPendingContinuation()) {
				this.getDialogues().clickContinue();
			}
			
			if (this.targetHasDespawned() || this.targetHasMoved()) {
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
			   this.target.exists() &&
			   this.targetLastPosition != null &&
			   this.targetLastPosition.getX() == this.target.getX() &&
			   this.targetLastPosition.getY() == this.target.getY();
	}
	
	private boolean targetHasDespawned() {
		return this.target != null && !this.target.exists();
	}

	private boolean finishedDropping() {
		return this.isDropping && this.getInventory().isEmptyExcept(this.fishingTools, "Feather");
	}
	
	private boolean isFishing() {
		return this.myPlayer().isAnimating() && this.target != null;
	}
	
	private boolean isMovingToFishingSpot() {
		return !this.targetHasDespawned() && !this.isFishing() && this.myPlayer().isMoving();
	}
	
	private boolean isReadyToDrop() {
		return this.getInventory().isFull() && !this.isDropping;
	}
	
	private boolean isReadyToFish() {
		return !this.isDropping &&
			   this.target == null &&
			   !this.myPlayer().isAnimating() &&
			   !this.myPlayer().isMoving();
	}
	
	public final String formatTime(final long ms){
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60; h %= 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
	
	@Override
	public void onExit() {
		log("Thanks for running " + this.getName() + "!");
	}

	@Override
	public void onPaint(Graphics2D g) {
		final long runTime = System.currentTimeMillis() - this.startTime;
		g.setColor(Color.WHITE);
		g.drawString(this.getName() + " v" + this.getVersion(), 10, 25);
		g.drawString("Status: " + this.status, 10, 40);
		g.drawString("Fishing XP: " + this.getExperienceTracker().getGainedXP(Skill.FISHING), 10, 55);
		g.drawString("Fishing lvls gained: " + this.getExperienceTracker().getGainedLevels(Skill.FISHING), 10, 70);
		g.drawString("Fishing lvl: " + this.getSkills().getStatic(Skill.FISHING), 10, 85);
		g.drawString("Running for: " + this.formatTime(runTime), 10, 100);
	}

}

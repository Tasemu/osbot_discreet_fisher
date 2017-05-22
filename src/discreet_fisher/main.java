package discreet_fisher;

import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

import java.awt.*;

@ScriptManifest(author = "Tasemu", info = "Simple Power Fisher", name = "Discreet Fisher", version = 1.0, logo = "")
public class main extends Script {
	private String thread = "https://osbot.org/forum/topic/58775";
	private NPC target;
	private boolean isDropping = false;

	@Override
	public void onStart() {
		log("Welcome to " + this.getName() + " v" + this.getVersion());
		log("Report any issues and bugs here: " + this.thread);
	}

	private enum State {
		FISH,
		DROP,
		WAIT
	};

	private State getState() {
		NPC fishSpot = this.getNpcs().closest("Fishing spot");
		
		if (fishSpot == null) {
			log("Cannot find fishing spot. Logging out and stopping script.");
			this.target = null;
			this.stop();
		}
		
		if (this.getInventory().isFull() && !this.isDropping) {
			return State.DROP;
		}
		
		if (!this.myPlayer().isAnimating() && !this.isDropping) {
			return State.FISH;
		}
		
		return State.WAIT;
	}

	@Override
	public int onLoop() throws InterruptedException {
		switch (getState()) {
		case DROP:
			this.getInventory().dropAllExcept("net");
			this.isDropping = true;
			
			break;
		case FISH:
			this.target = this.getNpcs().closest("Fishing spot");
			this.isDropping = false;
			
			if (this.target != null) {
				this.target.interact("use-net");
			}
			
			break;
		case WAIT:
			if (this.target != null && !this.target.exists()) {
				this.target = null;
			}
			
			if (this.isDropping && this.getInventory().isEmpty()) {
				this.isDropping = false;
			}
			
			break;
		}
		return random(200, 300);
	}

	@Override
	public void onExit() {
		log("Thanks for running " + this.getName() + "!");
	}

	@Override
	public void onPaint(Graphics2D g) {

	}

}

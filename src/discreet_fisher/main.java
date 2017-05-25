package discreet_fisher;

import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;

import java.awt.*;
import javax.swing.*;

@ScriptManifest(author = "Tasemu", info = "Simple Power Fisher", name = "Discreet Fisher", version = 1.0, logo = "")
public class main extends Script {
	private String thread = "https://osbot.org/forum/topic/58775";
	private NPC target;
	private boolean isDropping = false;
	private String fishAction = "Lure";
	private boolean useBait = false;
	private String bankAction = "Drop";
	private String fishingTools = "Fly fishing rod";
	private String bait = "Feather";
	private Position targetLastPosition;
	private long startTime;
	private String status = "Initializing";
	private JFrame gui;
	private boolean started = false;
	
	/**
	 * @wbp.parser.entryPoint
	 */
	private void createGUI() {
		this.gui = new JFrame("Discreet Fisher Options");
		gui.getContentPane().setBackground(Color.DARK_GRAY);
		gui.getContentPane().setLayout(null);
		
		JLabel lblFishingType = new JLabel("Fishing Type:");
		lblFishingType.setForeground(Color.WHITE);
		lblFishingType.setBounds(23, 18, 88, 16);
		gui.getContentPane().add(lblFishingType);
		
		JComboBox<String> comboBox = new JComboBox<String>();
		comboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"Net", "Lure", "Bait", "Cage", "Harpoon"}));
		comboBox.addActionListener(e -> this.fishAction = comboBox.getSelectedItem().toString());
		comboBox.setBounds(112, 14, 118, 27);
		gui.getContentPane().add(comboBox);
		
		JCheckBox chckbxUseBait = new JCheckBox("Use Bait?");
		chckbxUseBait.addActionListener(e -> this.useBait = chckbxUseBait.isSelected());
		chckbxUseBait.setForeground(Color.WHITE);
		chckbxUseBait.setBounds(16, 84, 128, 23);
		gui.getContentPane().add(chckbxUseBait);
		
		JRadioButton rdbtnDropFish = new JRadioButton("Drop Fish");
		rdbtnDropFish.setForeground(Color.WHITE);
		rdbtnDropFish.setSelected(true);
		rdbtnDropFish.setBounds(112, 84, 141, 23);
		gui.getContentPane().add(rdbtnDropFish);
		
		JRadioButton rdbtnBankFish = new JRadioButton("Bank Fish");
		rdbtnBankFish.setForeground(Color.WHITE);
		rdbtnBankFish.setBounds(209, 84, 141, 23);
		gui.getContentPane().add(rdbtnBankFish);
		
		rdbtnDropFish.addActionListener(e -> {
			this.bankAction = "Drop";
			rdbtnBankFish.setSelected(false);;
		});
		
		rdbtnBankFish.addActionListener(e -> {
			this.bankAction = "Bank";
			rdbtnDropFish.setSelected(false);
		});
		
		JLabel lblBaitType = new JLabel("Bait Type:");
		lblBaitType.setForeground(Color.WHITE);
		lblBaitType.setBounds(23, 56, 61, 16);
		gui.getContentPane().add(lblBaitType);
		
		JComboBox<String> comboBox_1 = new JComboBox<String>();
		comboBox_1.setModel(new DefaultComboBoxModel<String>(new String[] {"Feather", "Bait"}));
		comboBox_1.addActionListener(e -> this.bait = comboBox_1.getSelectedItem().toString());
		comboBox_1.setBounds(112, 52, 118, 27);
		gui.getContentPane().add(comboBox_1);
		
		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(e -> {
			this.started = true;
			gui.setVisible(false);
		});
		btnStart.setBounds(16, 119, 117, 29);
		gui.getContentPane().add(btnStart);
		
		this.gui.setVisible(true);
	}

	@Override
	public void onStart() {
		log("Welcome to " + this.getName() + " v" + this.getVersion());
		log("Report any issues and bugs here: " + this.thread);
		this.getExperienceTracker().start(Skill.FISHING);
		this.startTime = System.currentTimeMillis();
		this.createGUI();
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
		if (!this.started)
			return 0;
		
		if (!this.getClient().isLoggedIn() && !this.myPlayer().isVisible())
			return random(600, 1000);
		
		switch (getState()) {
		case MOVE:
			this.status = "Moving to fishing spot";
			
			break;
		case DROP:
			this.status = "Dropping fish";
			
			if (this.useBait) {
				this.getInventory().dropAllExcept(this.fishingTools, this.bait);
			} else {
				this.getInventory().dropAllExcept(this.fishingTools);
			}
				
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
			
			if (this.getInventory().getItem(this.bait) == null) {
				log("Out of bait, quitting script.");
				this.stop();
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
		if (this.useBait) {
			return this.isDropping && this.getInventory().isEmptyExcept(this.fishingTools, this.bait);
		} else {
			return this.isDropping && this.getInventory().isEmptyExcept(this.fishingTools);
		}
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
		if (this.gui != null) {
			this.gui.setVisible(false);
			this.gui.dispose();
		}
	}

	@Override
	public void onPaint(Graphics2D g) {
		if (this.started) {
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
}

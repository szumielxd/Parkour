package me.A5H73Y.Parkour.Course;

import java.io.Serializable;

import me.A5H73Y.Parkour.Other.Constants;
import me.A5H73Y.Parkour.Other.ParkourKit;

public class Course implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
    private Checkpoint checkpoint;
    private Integer maxDeaths;
	private int checkpoints;

	// Because of the Material Enum changes in 1.13 this will be transient
    // To stop the errors occurring when loading the ParkourSessions the
    // ParkourKit will be loaded on startup and set manually
    private transient ParkourKit parkourKit;

	public Course(String name, Checkpoint checkpoint, ParkourKit parkourKit) {
        this.name = name;
        this.checkpoint = checkpoint;
        this.parkourKit = parkourKit;
        this.checkpoints = CourseInfo.getCheckpointAmount(name);
    }

	public Course(String name, Checkpoint checkpoint) {
		this(name, checkpoint, ParkourKit.getParkourKit(Constants.DEFAULT));
	}

    /**
     * Get course's unique name
     * This will be lowercase
     * @return Course Name
     */
	public String getName() {
		return name;
	}

    /**
     * The current checkpoint achieved
     * @return Checkpoint
     */
	public Checkpoint getCurrentCheckpoint() {
		return checkpoint;
	}

	public void setMaxDeaths(Integer maxDeaths) {
		this.maxDeaths = maxDeaths;
	}

    /**
     * Maximum number of deaths a player can accumulate before failing the course
     * @return maximum deaths for course
     */
	public Integer getMaxDeaths() {
		return maxDeaths;
	}

    /**
     * ParkourKit for the course
     * This will be loaded when the course object is created
     * @return ParkourKit
     */
	public ParkourKit getParkourKit() {
		return parkourKit;
	}

	public void setParkourKit(ParkourKit parkourKit) {
		this.parkourKit = parkourKit;
	}

    /**
     * Number of checkpoints on the course
     * @return Count of Course's checkpoints
     */
	public int getCheckpoints() {
		return checkpoints;
	}

	public void setCheckpoint(Checkpoint checkpoint) {
		this.checkpoint = checkpoint;
	}

    public boolean hasMaxDeaths() {
	    return maxDeaths != null;
    }
}

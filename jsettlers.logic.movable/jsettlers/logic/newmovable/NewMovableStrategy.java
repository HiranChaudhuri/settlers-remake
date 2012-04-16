package jsettlers.logic.newmovable;

import java.io.Serializable;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EAction;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IMovable;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.newmovable.interfaces.IStrategyGrid;
import jsettlers.logic.newmovable.strategies.BearerMovableStrategy;
import jsettlers.logic.newmovable.strategies.BricklayerStrategy;
import jsettlers.logic.newmovable.strategies.BuildingWorkerStrategy;
import jsettlers.logic.newmovable.strategies.DiggerStrategy;
import jsettlers.logic.newmovable.strategies.TestMovableStrategy;
import jsettlers.logic.newmovable.strategies.soldiers.SoldierStrategy;
import jsettlers.logic.newmovable.strategies.specialists.DummySpecialistStrategy;
import jsettlers.logic.newmovable.strategies.specialists.PioneerStrategy;

public abstract class NewMovableStrategy implements Serializable {
	private static final long serialVersionUID = 3135655342562634378L;

	private final NewMovable movable;

	protected NewMovableStrategy(NewMovable movable) {
		this.movable = movable;
	}

	public static NewMovableStrategy getStrategy(NewMovable movable, EMovableType movableType) {
		switch (movableType) {
		case TEST_MOVABLE:
			return new TestMovableStrategy(movable);
		case BEARER:
			return new BearerMovableStrategy(movable);

		case SWORDSMAN_L1:
		case SWORDSMAN_L2:
		case SWORDSMAN_L3:
		case BOWMAN_L1:
		case BOWMAN_L2:
		case BOWMAN_L3:
		case PIKEMAN_L1:
		case PIKEMAN_L2:
		case PIKEMAN_L3:
			return new SoldierStrategy(movable, movableType);

		case BAKER:
		case CHARCOAL_BURNER:
		case FARMER:
		case FISHERMAN:
		case FORESTER:
		case MELTER:
		case MILLER:
		case MINER:
		case PIG_FARMER:
		case LUMBERJACK:
		case SAWMILLER:
		case SLAUGHTERER:
		case SMITH:
		case STONECUTTER:
		case WATERWORKER:
			return new BuildingWorkerStrategy(movable, movableType);

		case DIGGER:
			return new DiggerStrategy(movable);

		case BRICKLAYER:
			return new BricklayerStrategy(movable);

		case PIONEER:
			return new PioneerStrategy(movable);
		case GEOLOGIST:
		case THIEF:
			return new DummySpecialistStrategy(movable);

		default:
			assert false : "requested movableType: " + movableType + " but have no strategy for this type!";
			return null;
		}
	}

	protected abstract void action();

	protected final void convertTo(EMovableType movableType) {
		movable.convertTo(movableType);
	}

	protected final EMaterialType setMaterial(EMaterialType materialType) {
		return movable.setMaterial(materialType);
	}

	protected final void playAction(EAction movableAction, float duration) { // TODO @Andreas : rename EAction to EMovableAction
		movable.playAction(movableAction, duration);
	}

	protected final void lookInDirection(EDirection direction) {
		movable.lookInDirection(direction);
	}

	protected final boolean goToPos(ShortPoint2D targetPos) {
		return movable.goToPos(targetPos);
	}

	protected final IStrategyGrid getStrategyGrid() {
		return movable.getStrategyGrid();
	}

	/**
	 * Tries to go a step in the given direction.
	 * 
	 * @param direction
	 *            direction to go
	 * @return true if the step can and will immediately be executed. <br>
	 *         false if the target position is generally blocked or a movable occupies that position.
	 */
	protected final boolean goInDirection(EDirection direction) {
		return movable.goInDirection(direction);
	}

	/**
	 * Forces the movable to go a step in the given direction (if it is not blocked).
	 * 
	 * @param direction
	 *            direction to go
	 * @return true if the step can and will immediately be executed. <br>
	 *         false if the target position is blocked for this movable.
	 */
	protected final boolean forceGoInDirection(EDirection direction) {
		return movable.forceGoInDirection(direction);
	}

	protected final void setPosition(ShortPoint2D pos) {
		movable.setPos(pos);
	}

	protected final void setVisible(boolean visible) {
		movable.setVisible(visible);
	}

	/**
	 * 
	 * @param dijkstra
	 *            if true, dijkstra algorithm is used<br>
	 *            if false, in area finder is used.
	 * @param centerX
	 * @param centerY
	 * @param radius
	 * @param searchType
	 * @return
	 */
	protected final boolean preSearchPath(boolean dijkstra, short centerX, short centerY, short radius, ESearchType searchType) {
		return movable.preSearchPath(dijkstra, centerX, centerY, radius, searchType);
	}

	protected final void followPresearchedPath() {
		movable.followPresearchedPath();
	}

	protected final void enableNothingToDoAction(boolean enable) {
		movable.enableNothingToDoAction(enable);
	}

	protected void setSelected(boolean selected) {
		movable.setSelected(selected);
	}

	protected final boolean fitsSearchType(ShortPoint2D pos, ESearchType searchType) {
		return movable.getStrategyGrid().fitsSearchType(movable, pos, searchType);
	}

	protected final boolean isValidPosition(ShortPoint2D position) {
		return movable.isValidPosition(position);
	}

	public final ShortPoint2D getPos() {
		return movable.getPos();
	}

	protected final EMaterialType getMaterial() {
		return movable.getMaterial();
	}

	protected final byte getPlayer() {
		return movable.getPlayer();
	}

	protected IMovable getMovable() {
		return movable;
	}

	/**
	 * Checks preconditions before the next path step can be gone.
	 * 
	 * @param pathTarget
	 *            TODO
	 * 
	 * @return true if the path should be continued<br>
	 *         false if it must be stopped.
	 */
	protected boolean checkPathStepPreconditions(@SuppressWarnings("unused") ShortPoint2D pathTarget) {
		return true;
	}

	/**
	 * This method is called when a movable is killed and can be used for finalization.
	 * 
	 * @param pathTarget
	 *            if the movable is currently walking on a path, this is the target of the path<br>
	 *            else it is null.
	 */
	protected void killedEvent(@SuppressWarnings("unused") ShortPoint2D pathTarget) { // used in overriding methods
	}

	protected void moveToPathSet(@SuppressWarnings("unused") ShortPoint2D targetPos) {
	}

}

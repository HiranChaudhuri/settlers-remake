package jsettlers.logic.movable.specialist;

import java.util.BitSet;

import jsettlers.algorithms.path.Path;
import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.interfaces.IFerryMovable;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IThiefMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class ThiefMovable extends AttackableHumanMovable implements IThiefMovable {

	private BitSet uncoveredBy = new BitSet();

	private static final float ACTION1_DURATION = 1f;

	private EMoveToType nextMoveToType;
	private ShortPoint2D nextTarget = null;
	private ShortPoint2D currentTarget = null;
	private ShortPoint2D goToTarget = null;

	private ShortPoint2D returnPos = null;

	public ThiefMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.THIEF, position, player, movable, behaviour);
	}

	private static final Root<ThiefMovable> behaviour = new Root<>(createThiefBehaviour());

	public static Node<ThiefMovable> createThiefBehaviour() {
		return guardSelector(
				guard(mov -> mov.nextTarget != null,
					BehaviorTreeHelper.action(mov -> {
						// steal something
						if(mov.nextMoveToType.isWorkOnDestination() && mov.getMaterial() == EMaterialType.NO_MATERIAL) {
							Path dijkstraPath = mov.grid.searchDijkstra(mov, mov.nextTarget.x, mov.nextTarget.y, (short) 30, ESearchType.FOREIGN_MATERIAL);
							if (dijkstraPath != null) {
								mov.currentTarget = dijkstraPath.getTargetPosition();
							} else {
								mov.currentTarget = null;
							}

							mov.returnPos = mov.position;
						} else {
							// or just go there
							mov.goToTarget = mov.nextTarget;
						}
						mov.nextTarget = null;
					})
				),
				guard(mov -> mov.goToTarget != null,
						resetAfter(
								mov -> mov.goToTarget = null,
								goToPos(mov -> mov.goToTarget, mov -> mov.nextTarget == null && mov.goToTarget != null) // TODO
						)
				),
				guard(mov -> (mov.getMaterial() != EMaterialType.NO_MATERIAL && mov.isOnOwnGround()),
					BehaviorTreeHelper.action(ThiefMovable::dropMaterialIfPossible)
				),
				guard(mov -> mov.currentTarget != null,
					sequence(
						selector(
							condition(mov -> mov.position.equals(mov.currentTarget)),
							goToPos(mov -> mov.currentTarget, mov -> mov.currentTarget != null && (mov.getMaterial() == EMaterialType.NO_MATERIAL || !mov.isOnOwnGround()) && mov.nextTarget == null) // TODO
						),
						selector(
							condition(mov -> mov.getMaterial() != EMaterialType.NO_MATERIAL),
							sequence(
								stealMaterial(),
								BehaviorTreeHelper.action(mov -> {
									mov.goToTarget = mov.returnPos;
								})
							)
						)
					)
				)
		);
	}

	private void dropMaterialIfPossible() {
		EMaterialType stolenMaterial = setMaterial(EMaterialType.NO_MATERIAL);
		drop(stolenMaterial);
	}

	protected static Node<ThiefMovable> stealMaterial() {
		return sequence(
				condition(mov -> mov.grid.fitsSearchType(mov, mov.currentTarget.x, mov.currentTarget.y, ESearchType.FOREIGN_MATERIAL)),

				playAction(EMovableAction.ACTION1, mov -> (short)(ACTION1_DURATION*1000)),

				condition(mov -> {
					EMaterialType stolenMaterial = mov.grid.takeMaterial(mov.currentTarget);
					mov.setMaterial(stolenMaterial);
					return stolenMaterial != EMaterialType.NO_MATERIAL;
				})
		);
	}


	@Override
	public boolean isUncoveredBy(byte teamId) {
		return uncoveredBy.get(teamId);
	}

	@Override
	public void uncoveredBy(byte teamId) {
		uncoveredBy.set(teamId);
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		dropMaterialIfPossible();
	}

	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
		if(playerControlled && (!targetPosition.equals(currentTarget) || nextMoveToType != moveToType)) {
			nextTarget = targetPosition;
			nextMoveToType = moveToType;
		}
	}

	@Override
	public void moveToFerry(IFerryMovable ferry, ShortPoint2D entrancePosition) {
		if(playerControlled && (!entrancePosition.equals(currentTarget) || ferryToEnter != ferry)) {
			ferryToEnter = ferry;
			nextTarget = entrancePosition;
			nextMoveToType = EMoveToType.FORCED;
		}
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		nextTarget = position;
		nextMoveToType = stop? EMoveToType.FORCED : EMoveToType.DEFAULT;
	}
}

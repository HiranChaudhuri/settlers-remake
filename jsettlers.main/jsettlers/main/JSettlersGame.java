package jsettlers.main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jsettlers.common.map.IGraphicsGrid;
import jsettlers.common.map.MapLoadException;
import jsettlers.common.resources.ResourceManager;
import jsettlers.common.statistics.IStatisticable;
import jsettlers.graphics.map.MapInterfaceConnector;
import jsettlers.graphics.map.draw.ImageProvider;
import jsettlers.graphics.progress.EProgressState;
import jsettlers.graphics.startscreen.interfaces.EGameError;
import jsettlers.graphics.startscreen.interfaces.IGameExitListener;
import jsettlers.graphics.startscreen.interfaces.IStartedGame;
import jsettlers.graphics.startscreen.interfaces.IStartingGame;
import jsettlers.graphics.startscreen.interfaces.IStartingGameListener;
import jsettlers.input.GuiInterface;
import jsettlers.input.IGameStoppable;
import jsettlers.input.UIState;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.newGrid.MainGrid;
import jsettlers.logic.map.save.IGameCreator;
import jsettlers.logic.newmovable.NewMovable;
import jsettlers.logic.statistics.GameStatistics;
import jsettlers.logic.timer.MovableTimer;
import jsettlers.logic.timer.PartitionManagerTimer;
import jsettlers.logic.timer.Timer100Milli;
import networklib.client.OfflineTaskScheduler;
import networklib.client.interfaces.IGameClock;
import networklib.client.interfaces.ITaskScheduler;
import networklib.synchronic.random.RandomSingleton;

/**
 * This class can start a Thread that loads and sets up a game and wait's for its termination.
 * 
 * @author Andreas Eberle
 */
public class JSettlersGame {
	private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private final Object stopMutex = new Object();

	private final IGameCreator mapcreator;
	private final long randomSeed;
	private final byte playerNumber;
	private final ITaskScheduler taskScheduler;
	private final boolean multiplayer;
	private final File loadableReplayFile;

	private final GameRunner gameRunner;

	private boolean stopped = false;
	private boolean started = false;

	private JSettlersGame(IGameCreator mapCreator, long randomSeed,
			ITaskScheduler taskScheduler, byte playerNumber, boolean multiplayer, File loadableReplayFile) {
		this.mapcreator = mapCreator;
		this.randomSeed = randomSeed;
		this.taskScheduler = taskScheduler;
		this.playerNumber = playerNumber;
		this.multiplayer = multiplayer;
		this.loadableReplayFile = loadableReplayFile;

		this.gameRunner = new GameRunner();
	}

	public JSettlersGame(IGameCreator mapCreator, long randomSeed, ITaskScheduler taskScheduler, byte playerNumber) {
		this(mapCreator, randomSeed, taskScheduler, playerNumber, true, null);
	}

	/**
	 * Creates a new {@link JSettlersGame} object with an {@link OfflineTaskScheduler}.
	 * 
	 * @param mapCreator
	 * @param randomSeed
	 * @param playerNumber
	 */
	public JSettlersGame(IGameCreator mapCreator, long randomSeed, byte playerNumber) {
		this(mapCreator, randomSeed, playerNumber, null);
	}

	/**
	 * 
	 * @param mapCreator
	 * @param randomSeed
	 * @param playerNumber
	 */
	public JSettlersGame(IGameCreator mapCreator, long randomSeed, byte playerNumber, File loadableReplayFile) {
		this(mapCreator, randomSeed, new OfflineTaskScheduler(), playerNumber, false, loadableReplayFile);
	}

	/**
	 * Starts the game in a new thread. Returns immediately.
	 * 
	 * @return
	 */
	public synchronized IStartingGame start() {
		if (!started) {
			started = true;
			new Thread(null, gameRunner, "GameThread", 128 * 1024).start();
		}
		return gameRunner;
	}

	public void stop() {
		synchronized (stopMutex) {
			stopped = true;
			stopMutex.notifyAll();
		}
	}

	private class GameRunner implements Runnable, IStartingGame, IStartedGame, IGameStoppable {
		private IStartingGameListener startingGameListener;
		private MainGrid mainGrid;
		private GameStatistics statistics;
		private EProgressState progressState;
		private float progress;
		private IGameExitListener exitListener;

		@Override
		public void run() {
			try {
				updateProgressListener(EProgressState.LOADING, 0.1f);

				DataOutputStream replayFileStream = createReplayFileStream();

				IGameClock gameClock = MatchConstants.clock = taskScheduler.getGameClock();
				gameClock.setReplayLogStream(replayFileStream);
				RandomSingleton.load(randomSeed);

				updateProgressListener(EProgressState.LOADING_MAP, 0.3f);
				Thread imagePreloader = ImageProvider.getInstance().startPreloading();

				mainGrid = mapcreator.getMainGrid(playerNumber);
				UIState uiState = mapcreator.getUISettings(playerNumber);

				updateProgressListener(EProgressState.LOADING_IMAGES, 0.7f);
				statistics = new GameStatistics(gameClock);
				mainGrid.startThreads();

				imagePreloader.join(); // Wait for ImageProvider to finish loading the images

				waitForStartingGameListener();

				final MapInterfaceConnector connector = startingGameListener.startFinished(this);
				connector.loadUIState(uiState.getUiStateData());

				GuiInterface guiInterface = new GuiInterface(connector, gameClock, taskScheduler, mainGrid.getGuiInputGrid(), this, playerNumber,
						multiplayer);

				if (loadableReplayFile != null) {
					gameClock.loadReplayLogFromStream(new DataInputStream(new FileInputStream(loadableReplayFile.getAbsolutePath())));
				}

				gameClock.startExecution();

				synchronized (stopMutex) {
					while (!stopped) {
						try {
							stopMutex.wait();
						} catch (InterruptedException e) {
						}
					}
				}

				taskScheduler.shutdown();
				gameClock.stopExecution();
				connector.stop();
				mainGrid.stopThreads();
				guiInterface.stop();
				Timer100Milli.stop();
				MovableTimer.stop();
				PartitionManagerTimer.stop();
				NewMovable.dropAllMovables();
				Building.dropAllBuildings();
			} catch (MapLoadException e) {
				e.printStackTrace();
				reportFail(EGameError.MAPLOADING_ERROR, e);
			} catch (Exception e) {
				e.printStackTrace();
				reportFail(EGameError.UNKNOWN_ERROR, e);
			}
			if (exitListener != null) {
				exitListener.gameExited(this);
			}
		}

		private DataOutputStream createReplayFileStream() throws IOException {
			final String dateAndMap = logDateFormat.format(new Date()) + "_" + mapcreator.getMapName();
			final String replayFilename = "logs/" + dateAndMap + "/" + dateAndMap + "_replay.log";
			DataOutputStream replayFileStream = new DataOutputStream(ResourceManager.writeFile(replayFilename));

			ReplayStartInformation replayInfo = new ReplayStartInformation(randomSeed, playerNumber, mapcreator.getMapName(), mapcreator.getMapID());
			replayInfo.serialize(replayFileStream);
			replayFileStream.flush();

			return replayFileStream;
		}

		/**
		 * Waits until the {@link #startingGameListener} has been set.
		 */
		private void waitForStartingGameListener() {
			while (startingGameListener == null) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
				}
			}
		}

		private void updateProgressListener(EProgressState progressState,
				float progress) {
			this.progressState = progressState;
			this.progress = progress;

			if (startingGameListener != null)
				startingGameListener.startProgressChanged(progressState, progress);
		}

		private void reportFail(EGameError gameError, Exception e) {
			if (startingGameListener != null)
				startingGameListener.startFailed(gameError, e);
		}

		// METHODS of IStartingGame
		// ====================================================
		@Override
		public void setListener(IStartingGameListener startingGameListener) {
			this.startingGameListener = startingGameListener;
			if (startingGameListener != null)
				startingGameListener.startProgressChanged(progressState, progress);
		}

		@Override
		public void abort() {
			stop();
		}

		// METHODS of IStartedGame
		// ======================================================
		@Override
		public IGraphicsGrid getMap() {
			return mainGrid.getGraphicsGrid();
		}

		@Override
		public IStatisticable getPlayerStatistics() {
			return statistics;
		}

		@Override
		public int getPlayer() {
			return playerNumber;
		}

		@Override
		public void stopGame() {
			stop();
		}

		@Override
		public void setGameExitListener(IGameExitListener exitListener) {
			this.exitListener = exitListener;
		}
	}
}

package com.wissen.race;

import java.util.ArrayList;
import java.util.Random;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

/**
 * RaceView: implementation of a simple game of Race
 * @author wissen16(Mayur Birari)
 * 
 */
public class RaceView extends TileView {

	/**
	 * Current mode of application: READY to run, RUNNING, or you have already
	 * lost. static final ints are used. 
	 */
	private int mMode = READY;
	public static final int PAUSE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int LOSE = 3;
	public static final int WIN = 4;

	/**
	 * Current direction the race is headed.
	 */
	private int mDirection = NORTH;
	private int mNextDirection = NORTH;
	private static final int NORTH = 1;
	private static final int SOUTH = 2;
	private static final int EAST = 3;
	private static final int WEST = 4;
	private static int updateCount1 = 0;
	private static int updateCount2 = 0;
	private static int updateCount3 = 0;
	private static int updateCount4 = 0;

	/**
	 * Labels for the drawables that will be loaded into the TileView class
	 */
	private static final int RED_STAR = 1;
	private static final int YELLOW_STAR = 2;
	private static final int GREEN_STAR = 3;

	/**
	 * this flags controlling every moving objects
	 */
	private static boolean wallFlag1 = false;
	private static boolean wallFlag2 = false;
	private static boolean carMoveFlag = true;
	private static boolean newCarsFlag = true;
	private static boolean bulletflag = false;

	/**
	 * mScore: used to track the time captured mMoveDelay: number of
	 * milliseconds between car movements. This will decrease as scores are
	 * increased.
	 */
	private long mScore = 0;
	private long mMoveDelay = 600;
	/**
	 * mLastMove: tracks the absolute time when the main car last moved, and is
	 * used to determine if a move should be made based on mMoveDelay.
	 */
	private long mLastMove;

	/**
	 * mStatusText: text shows to the user in some run states
	 */
	private TextView mStatusText;

	/**
	 * mCarTrail: a list of Coordinates that make up the cars's body
	 * mCarList1: a list of Coordinates that make up the first cars's body
	 * mCarList2: a list of Coordinates that make up the second cars's body
	 * mCarList3: a list of Coordinates that make up the third cars's body
	 * mCarList4: a list of Coordinates that make up the forth cars's body
	 * BulletList: a list of Coordinates that make up the bullet fired by the car
	 */
	private ArrayList<Coordinate> mCarTrail = new ArrayList<Coordinate>();
	private ArrayList<Coordinate> mCarList1 = new ArrayList<Coordinate>();
	private ArrayList<Coordinate> mCarList2 = new ArrayList<Coordinate>();
	private ArrayList<Coordinate> mCarList3 = new ArrayList<Coordinate>();
	private ArrayList<Coordinate> mCarList4 = new ArrayList<Coordinate>();
	private ArrayList<Coordinate> BulletList = new ArrayList<Coordinate>();

	/**
	 * needs a little randomness in the car race
	 */
	private static final Random RNG = new Random();

	/**
	 * Create a simple handler that we can use to cause animation to happen. We
	 * set ourselves as a target and we can use the sleep() function to cause an
	 * update/invalidate to occur at a later date.
	 */
	private RefreshHandler mRedrawHandler = new RefreshHandler();

	class RefreshHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			RaceView.this.update();
			RaceView.this.invalidate();
		}

		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};

	/**
	 * Constructs a RaceView based on inflation from XML
	 * 
	 * @param context
	 * @param attrs
	 */
	public RaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initRaceView();
	}

	public RaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initRaceView();
	}

	private void initRaceView() {
		setFocusable(true);
		Resources r = this.getContext().getResources();
		resetTiles(4);
		loadTile(RED_STAR, r.getDrawable(R.drawable.redstar));
		loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
		loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar));

	}

	private void initNewGame() {
		mCarTrail.clear();
		mCarList1.clear();
		mCarList2.clear();
		mCarList3.clear();
		mCarList4.clear();
		BulletList.clear();

		
        //here i create intial position for car
		mCarTrail.add(new Coordinate(21, 28));
		mCarTrail.add(new Coordinate(20, 29));
		mCarTrail.add(new Coordinate(21, 29));
		mCarTrail.add(new Coordinate(22, 29));
		mCarTrail.add(new Coordinate(21, 30));
		mCarTrail.add(new Coordinate(21, 31));
		mCarTrail.add(new Coordinate(20, 32));
		mCarTrail.add(new Coordinate(21, 32));
		mCarTrail.add(new Coordinate(22, 32));
		mCarTrail.add(new Coordinate(21, 33));
		mNextDirection = NORTH;

		mMoveDelay = 100;
		mScore = 0;
	}

	/**
	 * Given a ArrayList of coordinates, we need to flatten them into an array
	 * of ints before we can stuff them into a map for flattening and storage.
	 * 
	 * @param cvec
	 *            : a ArrayList of Coordinate objects
	 * @return : a simple array containing the x/y values of the coordinates as
	 *         [x1,y1,x2,y2,x3,y3...]
	 */
	private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
		int count = cvec.size();
		int[] rawArray = new int[count * 2];
		for (int index = 0; index < count; index++) {
			Coordinate c = cvec.get(index);
			rawArray[2 * index] = c.x;
			rawArray[2 * index + 1] = c.y;
		}
		return rawArray;
	}

	/**
	 * Save game state so that the user does not lose anything if the game
	 * process is killed while we are in the background.
	 * 
	 * @return a Bundle with this view's state
	 */
	public Bundle saveState() {
		Bundle map = new Bundle();

		map.putIntArray("mCarList1", coordArrayListToArray(mCarList1));
		map.putIntArray("mCarList2", coordArrayListToArray(mCarList2));
		map.putIntArray("mCarList3", coordArrayListToArray(mCarList3));
		map.putIntArray("mCarList4", coordArrayListToArray(mCarList4));
		map.putIntArray("BulletList", coordArrayListToArray(BulletList));
		map.putInt("mDirection", Integer.valueOf(mDirection));
		map.putInt("mNextDirection", Integer.valueOf(mNextDirection));
		map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
		map.putLong("mScore", Long.valueOf(mScore));
		map.putIntArray("mCarTrail", coordArrayListToArray(mCarTrail));
		map.putInt("updateCount1", Integer.valueOf(updateCount1));
		map.putInt("updateCount2", Integer.valueOf(updateCount2));
		map.putInt("updateCount3", Integer.valueOf(updateCount3));
		map.putInt("updateCount4", Integer.valueOf(updateCount4));

		return map;
	}

	/**
	 * Given a flattened array of ordinate pairs, we reconstitute them into a
	 * ArrayList of Coordinate objects
	 * 
	 * @param rawArray
	 *            : [x1,y1,x2,y2,...]
	 * @return a ArrayList of Coordinates
	 */
	private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
		ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

		int coordCount = rawArray.length;
		for (int index = 0; index < coordCount; index += 2) {
			Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
			coordArrayList.add(c);
		}
		return coordArrayList;
	}

	/**
	 * Restore game state if our process is being relaunched
	 * 
	 * @param icicle
	 *            a Bundle containing the game state
	 */
	public void restoreState(Bundle icicle) {
		setMode(PAUSE);

		mCarList1 = coordArrayToArrayList(icicle.getIntArray("mCarList1"));
		mCarList2 = coordArrayToArrayList(icicle.getIntArray("mCarList2"));
		mCarList3 = coordArrayToArrayList(icicle.getIntArray("mCarList3"));
		mCarList4 = coordArrayToArrayList(icicle.getIntArray("mCarList4"));
		BulletList = coordArrayToArrayList(icicle.getIntArray("BulletList"));
		mDirection = icicle.getInt("mDirection");
		mNextDirection = icicle.getInt("mNextDirection");
		mMoveDelay = icicle.getLong("mMoveDelay");
		mScore = icicle.getLong("mScore");
		mCarTrail = coordArrayToArrayList(icicle.getIntArray("mCarTrail"));
		updateCount1 = icicle.getInt("updateCount1");
		updateCount2 = icicle.getInt("updateCount2");
		updateCount3 = icicle.getInt("updateCount3");
		updateCount4 = icicle.getInt("updateCount4");
	}

	/*
	 * handles key events in the game. Update the direction of cars
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		carMoveFlag = true;
		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if (mMode == READY) {
				/*
				 * At the beginning of the game, or the end of a previous one,
				 * we should start a new game.
				 */
				initNewGame();
				setMode(RUNNING);
				update();
				return (true);
			}
			if (mMode == LOSE) {
				System.exit(0);
			}
			if (mMode == PAUSE) {
				/*
				 * If the game is merely paused, we should just continue where
				 * we left off.
				 */
				initNewGame();
				setMode(RUNNING);
				update();
				return (true);
			}

			mNextDirection = NORTH;
			return (true);
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			mNextDirection = SOUTH;
			return (true);
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			mNextDirection = WEST;
			return (true);
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			mNextDirection = EAST;
			return (true);
		}

		return super.onKeyDown(keyCode, msg);
	}

	/**
	 * Sets the TextView that will be used to give information (such as "Game
	 * Over" to the user.
	 * 
	 * @param newView
	 */
	public void setTextView(TextView newView) {
		mStatusText = newView;
	}

	/**
	 * Updates the current mode of the application (RUNNING or PAUSED)
	 * as well as sets the visibility of textview for notification
	 * 
	 * @param newMode define mode of game 
	 */
	public void setMode(int newMode) {
		int oldMode = mMode;
		mMode = newMode;

		if (newMode == RUNNING & oldMode != RUNNING) {
			mStatusText.setVisibility(View.INVISIBLE);
			update();
			return;
		}

		Resources res = getContext().getResources();
		CharSequence str = "";
		if (newMode == PAUSE) {
			str = res.getText(R.string.mode_pause);
			System.exit(0);
		}
		if (newMode == READY) {
			str = res.getText(R.string.mode_ready);
		}
		if (newMode == WIN) {
			str = res.getText(R.string.mode_win);
		}
		if (newMode == LOSE) {
			str = res.getString(R.string.mode_lose_prefix) + mScore
					+ res.getString(R.string.mode_lose_suffix);
		}

		mStatusText.setText(str);
		mStatusText.setVisibility(View.VISIBLE);

	}

	/**
	 * Handles the basic update loop, checking to see if we are in the running
	 * state, determining if a move should be made, updating the main car's
	 * and opponent car's location.
	 */
	public void update() {
		if (mMode == RUNNING) {
			long now = System.currentTimeMillis();

			if (now - mLastMove > mMoveDelay) {
				clearTiles();
				updateWalls();
				if (mScore < 500)
					updateComCar();
				updateCar();
				mLastMove = now;
			}
			mRedrawHandler.sleep(mMoveDelay);
		}

	}

	/**
	 * Draws some walls to the both sides of the road.
	 * 
	 */
	private void updateWalls() {
		if (wallFlag1) {
			for (int y = 0; y < mYTileCount - 1; y += 3) {
				setTile(RED_STAR, 0, y);
				setTile(RED_STAR, mXTileCount - 1, y);
				setTile(YELLOW_STAR, 0, y + 1);
				setTile(YELLOW_STAR, mXTileCount - 1, y + 1);
				setTile(GREEN_STAR, 0, y + 2);
				setTile(GREEN_STAR, mXTileCount - 1, y + 2);
			}
			wallFlag1 = false;
		} else if (wallFlag2) {
			for (int y = 0; y < mYTileCount - 1; y += 3) {
				setTile(GREEN_STAR, 0, y);
				setTile(GREEN_STAR, mXTileCount - 1, y);
				setTile(RED_STAR, 0, y + 1);
				setTile(RED_STAR, mXTileCount - 1, y + 1);
				setTile(YELLOW_STAR, 0, y + 2);
				setTile(YELLOW_STAR, mXTileCount - 1, y + 2);
			}
			wallFlag2 = false;
		} else {
			for (int y = 0; y < mYTileCount - 1; y += 3) {
				setTile(YELLOW_STAR, 0, y);
				setTile(YELLOW_STAR, mXTileCount - 1, y);
				setTile(GREEN_STAR, 0, y + 1);
				setTile(GREEN_STAR, mXTileCount - 1, y + 1);
				setTile(RED_STAR, 0, y + 2);
				setTile(RED_STAR, mXTileCount - 1, y + 2);
			}
			wallFlag1 = true;
			wallFlag2 = true;
		}
	}
	/**
	 * to add random opponent car1 
	 */
	private void addRandomCar1() {
		updateCount1 = 0;
		Coordinate newCoord = null;
		// Choose a new location for com car
		int newX = 2 + RNG.nextInt(mXTileCount - 4);
		newCoord = new Coordinate(newX, 0);
		mCarList1.add(newCoord);
	}
	/**
	 * to add random opponent car2
	 */
	private void addRandomCar2() {
		updateCount2 = 0;
		Coordinate newCoord = null;
		// Choose a new location for com car
		int newX = 2 + RNG.nextInt(mXTileCount - 4);
		if (newCarsFlag) {
			newCoord = new Coordinate(newX, 7);
			updateCount2 = 7;
		} else
			newCoord = new Coordinate(newX, 0);
		mCarList2.add(newCoord);

	}
	/**
	 * to add random opponent car3
	 */
	private void addRandomCar3() {
		updateCount3 = 0;
		Coordinate newCoord = null;
		// Choose a new location for com car
		int newX = 2 + RNG.nextInt(mXTileCount - 4);
		if (newCarsFlag) {
			newCoord = new Coordinate(newX, 13);
			updateCount3 = 13;
		} else
			newCoord = new Coordinate(newX, 0);
		mCarList3.add(newCoord);

	}

	/**
	 * to add random opponent car4
	 */
	private void addRandomCar4() {
		updateCount4 = 0;
		Coordinate newCoord = null;
		// Choose a new location for com car
		int newX = 2 + RNG.nextInt(mXTileCount - 4);
		if (newCarsFlag) {
			newCoord = new Coordinate(newX, 20);
			updateCount4 = 20;
		} else
			newCoord = new Coordinate(newX, 0);
		mCarList4.add(newCoord);

	}

	/**
	 * Draws opponent cars and maintain their current positions .
	 * 
	 */
	private void updateComCar() {

		if (newCarsFlag) {
			addRandomCar1();
			addRandomCar2();
			addRandomCar3();
			addRandomCar4();
			newCarsFlag = false;
		}
		Coordinate car = mCarList1.get(0);
		mCarList1.clear();
		switch (updateCount1) {
		case 0:
		case 1:
			mCarList1.add(new Coordinate(car.x, car.y + 1));
			mCarList1.add(new Coordinate(car.x - 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y));
			mCarList1.add(new Coordinate(car.x + 1, car.y));
			break;
		case 2:
			mCarList1.add(new Coordinate(car.x, car.y + 1));
			mCarList1.add(new Coordinate(car.x - 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y));
			mCarList1.add(new Coordinate(car.x + 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y - 1));
			break;
		case 3:
			mCarList1.add(new Coordinate(car.x, car.y + 1));
			mCarList1.add(new Coordinate(car.x - 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y));
			mCarList1.add(new Coordinate(car.x + 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y - 1));
			mCarList1.add(new Coordinate(car.x, car.y - 2));
			break;
		case 4:
			mCarList1.add(new Coordinate(car.x, car.y + 1));
			mCarList1.add(new Coordinate(car.x - 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y));
			mCarList1.add(new Coordinate(car.x + 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y - 1));
			mCarList1.add(new Coordinate(car.x, car.y - 2));
			mCarList1.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList1.add(new Coordinate(car.x, car.y - 3));
			mCarList1.add(new Coordinate(car.x + 1, car.y - 3));
			break;
		case 35:
			addRandomCar1();
			break;
		default:
			mCarList1.add(new Coordinate(car.x, car.y + 1));
			mCarList1.add(new Coordinate(car.x - 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y));
			mCarList1.add(new Coordinate(car.x + 1, car.y));
			mCarList1.add(new Coordinate(car.x, car.y - 1));
			mCarList1.add(new Coordinate(car.x, car.y - 2));
			mCarList1.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList1.add(new Coordinate(car.x, car.y - 3));
			mCarList1.add(new Coordinate(car.x + 1, car.y - 3));
			mCarList1.add(new Coordinate(car.x, car.y - 4));
			break;
		} // switch
		updateCount1++;
		for (Coordinate c : mCarList1) {
			setTile(YELLOW_STAR, c.x, c.y);
		}

		car = mCarList2.get(0);
		mCarList2.clear();
		switch (updateCount2) {
		case 0:
		case 1:
			mCarList2.add(new Coordinate(car.x, car.y + 1));
			mCarList2.add(new Coordinate(car.x - 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y));
			mCarList2.add(new Coordinate(car.x + 1, car.y));
			break;
		case 2:
			mCarList2.add(new Coordinate(car.x, car.y + 1));
			mCarList2.add(new Coordinate(car.x - 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y));
			mCarList2.add(new Coordinate(car.x + 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y - 1));
			break;
		case 3:
			mCarList2.add(new Coordinate(car.x, car.y + 1));
			mCarList2.add(new Coordinate(car.x - 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y));
			mCarList2.add(new Coordinate(car.x + 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y - 1));
			mCarList2.add(new Coordinate(car.x, car.y - 2));
			break;
		case 4:
			mCarList2.add(new Coordinate(car.x, car.y + 1));
			mCarList2.add(new Coordinate(car.x - 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y));
			mCarList2.add(new Coordinate(car.x + 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y - 1));
			mCarList2.add(new Coordinate(car.x, car.y - 2));
			mCarList2.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList2.add(new Coordinate(car.x, car.y - 3));
			mCarList2.add(new Coordinate(car.x + 1, car.y - 3));
			break;
		case 35:
			addRandomCar2();
			break;
		default:
			mCarList2.add(new Coordinate(car.x, car.y + 1));
			mCarList2.add(new Coordinate(car.x - 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y));
			mCarList2.add(new Coordinate(car.x + 1, car.y));
			mCarList2.add(new Coordinate(car.x, car.y - 1));
			mCarList2.add(new Coordinate(car.x, car.y - 2));
			mCarList2.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList2.add(new Coordinate(car.x, car.y - 3));
			mCarList2.add(new Coordinate(car.x + 1, car.y - 3));
			mCarList2.add(new Coordinate(car.x, car.y - 4));
			break;
		} // switch
		updateCount2++;
		for (Coordinate c : mCarList2) {
			setTile(GREEN_STAR, c.x, c.y);
		}

		car = mCarList3.get(0);
		mCarList3.clear();
		switch (updateCount3) {
		case 0:
		case 1:
			mCarList3.add(new Coordinate(car.x, car.y + 1));
			mCarList3.add(new Coordinate(car.x - 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y));
			mCarList3.add(new Coordinate(car.x + 1, car.y));
			break;
		case 2:
			mCarList3.add(new Coordinate(car.x, car.y + 1));
			mCarList3.add(new Coordinate(car.x - 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y));
			mCarList3.add(new Coordinate(car.x + 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y - 1));
			break;
		case 3:
			mCarList3.add(new Coordinate(car.x, car.y + 1));
			mCarList3.add(new Coordinate(car.x - 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y));
			mCarList3.add(new Coordinate(car.x + 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y - 1));
			mCarList3.add(new Coordinate(car.x, car.y - 2));
			break;
		case 4:
			mCarList3.add(new Coordinate(car.x, car.y + 1));
			mCarList3.add(new Coordinate(car.x - 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y));
			mCarList3.add(new Coordinate(car.x + 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y - 1));
			mCarList3.add(new Coordinate(car.x, car.y - 2));
			mCarList3.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList3.add(new Coordinate(car.x, car.y - 3));
			mCarList3.add(new Coordinate(car.x + 1, car.y - 3));
			break;
		case 35:
			addRandomCar3();
			break;
		default:
			mCarList3.add(new Coordinate(car.x, car.y + 1));
			mCarList3.add(new Coordinate(car.x - 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y));
			mCarList3.add(new Coordinate(car.x + 1, car.y));
			mCarList3.add(new Coordinate(car.x, car.y - 1));
			mCarList3.add(new Coordinate(car.x, car.y - 2));
			mCarList3.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList3.add(new Coordinate(car.x, car.y - 3));
			mCarList3.add(new Coordinate(car.x + 1, car.y - 3));
			mCarList3.add(new Coordinate(car.x, car.y - 4));
			break;
		} // switch
		updateCount3++;
		for (Coordinate c : mCarList3) {
			setTile(RED_STAR, c.x, c.y);
		}

		car = mCarList4.get(0);
		mCarList4.clear();
		switch (updateCount4) {
		case 0:
		case 1:
			mCarList4.add(new Coordinate(car.x, car.y + 1));
			mCarList4.add(new Coordinate(car.x - 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y));
			mCarList4.add(new Coordinate(car.x + 1, car.y));
			break;
		case 2:
			mCarList4.add(new Coordinate(car.x, car.y + 1));
			mCarList4.add(new Coordinate(car.x - 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y));
			mCarList4.add(new Coordinate(car.x + 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y - 1));
			break;
		case 3:
			mCarList4.add(new Coordinate(car.x, car.y + 1));
			mCarList4.add(new Coordinate(car.x - 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y));
			mCarList4.add(new Coordinate(car.x + 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y - 1));
			mCarList4.add(new Coordinate(car.x, car.y - 2));
			break;
		case 4:
			mCarList4.add(new Coordinate(car.x, car.y + 1));
			mCarList4.add(new Coordinate(car.x - 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y));
			mCarList4.add(new Coordinate(car.x + 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y - 1));
			mCarList4.add(new Coordinate(car.x, car.y - 2));
			mCarList4.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList4.add(new Coordinate(car.x, car.y - 3));
			mCarList4.add(new Coordinate(car.x + 1, car.y - 3));
			break;
		case 35:
			addRandomCar4();
			break;
		default:
			mCarList4.add(new Coordinate(car.x, car.y + 1));
			mCarList4.add(new Coordinate(car.x - 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y));
			mCarList4.add(new Coordinate(car.x + 1, car.y));
			mCarList4.add(new Coordinate(car.x, car.y - 1));
			mCarList4.add(new Coordinate(car.x, car.y - 2));
			mCarList4.add(new Coordinate(car.x - 1, car.y - 3));
			mCarList4.add(new Coordinate(car.x, car.y - 3));
			mCarList4.add(new Coordinate(car.x + 1, car.y - 3));
			mCarList4.add(new Coordinate(car.x, car.y - 4));
			break;
		} // switch
		updateCount4++;
		for (Coordinate c : mCarList4) {
			setTile(YELLOW_STAR, c.x, c.y);
		}
	}

	/**
	 * Figure out which way the car is moving, see if he's run into anything
	 * (out off the road,collision with opponent ). If he's not going to die,then we move the car
	 * as per the direction is specified nad on up key event main car also send a bullet which destroy 
	 * the opponents.
	 */
	private void updateCar() {
		if (mScore > 500) {
			carMoveFlag = true;
			mNextDirection = NORTH;
			mCarList2.clear();
			mCarList3.clear();
			mCarList4.clear();
			BulletList.clear();
			bulletflag = false;
		}
		if (carMoveFlag) {

			Coordinate head = mCarTrail.get(0);
			Coordinate newHead = new Coordinate(1, 1);

			mDirection = mNextDirection;

			switch (mDirection) {
			case EAST: {
				newHead = new Coordinate(head.x + 2, head.y);
				break;
			}
			case WEST: {
				newHead = new Coordinate(head.x - 2, head.y);
				break;
			}
			case NORTH: {
				newHead = new Coordinate(head.x, head.y - 2);
				BulletList.clear();
				BulletList.add(newHead);
				bulletflag = true;
				break;
			}
			case SOUTH: {
				newHead = new Coordinate(head.x, head.y + 2);
				break;
			}
			}

			// Collision detection
			// For now we have a 1-square wall(border of a road) around the entire app
			if ((newHead.x < 2) || (newHead.y < 1)
					|| (newHead.x > mXTileCount - 2)
					|| (newHead.y > mYTileCount - 6)) {
				setMode(LOSE);
				return;
			}
			if (newHead.y < 4 && mScore > 500)
				setMode(WIN);				
				mCarTrail.clear();
			mCarTrail.add(newHead);
			mCarTrail.add(new Coordinate(newHead.x - 1, newHead.y + 1));
			mCarTrail.add(new Coordinate(newHead.x, newHead.y + 1));
			mCarTrail.add(new Coordinate(newHead.x + 1, newHead.y + 1));
			mCarTrail.add(new Coordinate(newHead.x, newHead.y + 2));
			mCarTrail.add(new Coordinate(newHead.x, newHead.y + 3));
			mCarTrail.add(new Coordinate(newHead.x - 1, newHead.y + 4));
			mCarTrail.add(new Coordinate(newHead.x, newHead.y + 4));
			mCarTrail.add(new Coordinate(newHead.x + 1, newHead.y + 4));
			mCarTrail.add(new Coordinate(newHead.x, newHead.y + 5));
			carMoveFlag = false;
		}
		int index = 0;
		for (Coordinate c : mCarTrail) {
			if (index == 0) {
				setTile(YELLOW_STAR, c.x, c.y);
			} else {
				setTile(RED_STAR, c.x, c.y);
			}
			index++;
		}
		mScore++;

		//if the bullet get fired
		if (bulletflag) {
			Coordinate bullet = BulletList.get(0);
			BulletList.remove(0);
			BulletList.add(new Coordinate(bullet.x, bullet.y - 1));
			setTile(RED_STAR, bullet.x, bullet.y);
			if (bullet.y < 2) {
				BulletList.clear();
				bulletflag = false;
			}
		}
		//on collision with opponent car or opponent car hit by bullet
		for (Coordinate mainc : mCarTrail) {
			for (Coordinate cy : mCarList1) {
				if (cy.equals(mainc)) {
					setMode(LOSE);
					return;
				}
				if (bulletflag)
					if (cy.equals(BulletList.get(0))) {
						while (updateCount1 != 35)
							updateCount1++;
						BulletList.clear();
						bulletflag = false;
						return;
					}
			}
			for (Coordinate cr : mCarList2) {
				if (cr.equals(mainc)) {
					setMode(LOSE);
					return;
				}
				if (bulletflag)
					if (cr.equals(BulletList.get(0))) {
						while (updateCount2 != 35)
							updateCount2++;
						BulletList.clear();
						bulletflag = false;
						return;
					}
			}
			for (Coordinate cg : mCarList3) {
				if (cg.equals(mainc)) {
					setMode(LOSE);
					return;
				}
				if (bulletflag)
					if (cg.equals(BulletList.get(0))) {
						while (updateCount3 != 35)
							updateCount3++;
						BulletList.clear();
						bulletflag = false;
						return;
					}
			}
			for (Coordinate cy : mCarList4) {
				if (cy.equals(mainc)) {
					setMode(LOSE);
					return;
				}
				if (bulletflag)
					if (cy.equals(BulletList.get(0))) {
						while (updateCount4 != 35)
							updateCount4++;
						BulletList.clear();
						bulletflag = false;
						return;
					}
			}
		}
	}

	/**
	 * Simple class containing two integer values and a comparison function.
	 * There's probably something I should use instead, but this was quick and
	 * easy to build.
	 * 
	 */
	private class Coordinate {
		public int x;
		public int y;

		public Coordinate(int newX, int newY) {
			x = newX;
			y = newY;
		}

		public boolean equals(Coordinate other) {
			if (x == other.x && y == other.y) {
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return "Coordinate: [" + x + "," + y + "]";
		}
	}

}

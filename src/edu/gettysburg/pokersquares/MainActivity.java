package edu.gettysburg.pokersquares;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.gettysburg.ai.NARLPokerSquaresPlayer;
import edu.gettysburg.ai.newPokerSquares;

public class MainActivity extends Activity implements View.OnClickListener, View.OnTouchListener {
	private HashMap<String, ImageView>  map    	= new HashMap<String, ImageView>();
	private HashMap<String, ImageView>  computerMap    	= new HashMap<String, ImageView>();
	private HashMap<String, TextView>   textMap = new HashMap<String, TextView>();
	private ArrayList<List<Card>> 		places  = new ArrayList<List<Card>>();
	private ArrayList<List<Card>> 		computerPlaces  = new ArrayList<List<Card>>();
	private Stack<Card> 				deck; 
	private Card[][] 					array   = new Card[5][5];
	private Card 			  			currentDeckCard;
	private TextView					textTotal, textTotalString;
	private ImageView  					deckView;
	private boolean 					isMuted = false;
	private boolean						isShowingAI = false;
	private boolean 					isAllowedToPress = false;
	private boolean						isAllowedToShow	= true;
	private int 						moves   = 0, gameTotal = 0, computerScore = 0, wins = 0, losses = 0, ties = 0, highestScore = 0;
	private MediaPlayer 				mp      = new MediaPlayer();
	private Menu 						menu = null;
	private String						userName = "";
	newPokerSquares computer;

	// swipe stuff
	private static final int NONE = 0;
	private static final int SWIPE = 1;
	private int mode = NONE;
	private float startY;
	private float stopY;
	private float startX;
	private float stopX;
	// We will only detect a swipe if the difference is at least 100 pixels
	// Change this value to your needs
	private static final int TRESHOLD = 75;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		View contentView = (View)findViewById(R.id.RelativeLayout1);
		contentView.setOnTouchListener((View.OnTouchListener)this);

		//System.out.println("Working Directory = " + System.getProperty("user.dir"));

		// Get userName from SplashScreen activity
		Bundle bundle = getIntent().getExtras();
		userName = bundle.getString("userName");

		try { // read the AI in from the trained player, or push the AI player into the local storage of the device
			openFileInput("narl.dat");
		} catch (FileNotFoundException e) {
			copyAssets();
			e.printStackTrace();
		}
		if(userName.equals("")) { userName = "Player"; }
		String FILENAME = userName+".txt";
		try {
			FileInputStream fis = openFileInput(FILENAME);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
			wins = Integer.valueOf(reader.readLine());
			losses = Integer.valueOf(reader.readLine());
			ties = Integer.valueOf(reader.readLine());
			highestScore = Integer.valueOf(reader.readLine());
			isMuted = Boolean.valueOf(reader.readLine());
		} catch (FileNotFoundException e) {
			System.err.println("File " + FILENAME + " does not exist");
		} catch (IOException e) {
			System.err.println("Error on reading data from " + FILENAME);
			e.printStackTrace();
		}

		// Get the profont font from the assets folder
		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/ProFontWindows.ttf");

		// Get resources for all of the vertical + horizontal textViews, 
		// 		then add it to the hashMap of textViews using its ID as the key
		for(int i=0; i<=9; i++) {
			int resourceID 	= getResources().getIdentifier("view"+i, "id", getPackageName());
			TextView toAdd = (TextView) findViewById(resourceID);
			if(i>=5) toAdd.setWidth(70);
			toAdd.getPaint().setAntiAlias(false);
			toAdd.setTypeface(tf);
			toAdd.setTextSize(12);
			//toAdd.setTextSize(12);
			textMap.put("view"+i, toAdd);
		}

		// set DPI-independent padding for the first linera layout (holding all of the horizontal text views)
		View tmp = (View) findViewById (getResources().getIdentifier("linearlayout0", "id", getPackageName()));
		tmp.setPadding(90, 0, 0, 0);

		// instantiate and set the typeface for the text total objects
		textTotal 		 = (TextView) findViewById(R.id.textTotal);
		textTotal.setTypeface(tf);
		textTotalString  = (TextView) findViewById(R.id.textTotalString);

		// Initialize card deck, then shuffle it to ensure randomness
		deck 			 = Card.initialize();
		Collections.shuffle(deck);

		// make a copy of the deck
		@SuppressWarnings("unchecked")
		Stack<Card> deckCopy = (Stack<Card>) deck.clone();
		// create computer AI player
		NARLPokerSquaresPlayer player = new NARLPokerSquaresPlayer();
		computer = new newPokerSquares(player, 60000, edu.gettysburg.ai.Card.interpret(deckCopy));
		//startService(new Intent(this, bgService.class).putExtra("deck", deck));

		// For clarity on colored backgrounds...
		textTotal.setShadowLayer(7, 0, 0, Color.BLACK);
		textTotalString.setShadowLayer(7, 0, 0, Color.BLACK);

		// so we can initialize the card faces with a cool pattern
		int counter = 2;
		// Get resources for all of the ImageViews, set their onClickListener, 
		//    and then add them to them hashMap of ImageViews using its ID as the key
		for(int r=1; r<6; r++) {
			for(int c=1; c<6; c++) {
				int resourceID 	= getResources().getIdentifier("r" + r + "c" + c, "id", getPackageName());
				ImageView toAdd = (ImageView) findViewById(resourceID);
				toAdd.setOnClickListener((View.OnClickListener)this);
				//toAdd.setCropToPadding(false);

				toAdd.setOnTouchListener(new OnTouchListener() {
					private Rect rect;

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						ImageView img = (ImageView) v;
						if(event.getAction() == MotionEvent.ACTION_DOWN){
							if(v.isClickable()) {
								img.setColorFilter(Color.argb(50, 0, 0, 0));
								rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
							}
						}
						if(event.getAction() == MotionEvent.ACTION_UP){
							img.setColorFilter(Color.argb(0, 0, 0, 0));
							v.performClick();
						}
						if(event.getAction() == MotionEvent.ACTION_MOVE){
							if(!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())){
								img.setColorFilter(Color.argb(0, 0, 0, 0));
							} 
						}

						return false;
					}
				});

				map.put("r" + r + "c" + c, toAdd);

				// dope ternary - you = jealous 
				Bitmap initialBmp = counter % 2 == 0 ? BitmapFactory.decodeResource(this.getResources(), R.drawable.topbvert): 
					BitmapFactory.decodeResource(this.getResources(), R.drawable.toprvert);

				initialBmp = Bitmap.createScaledBitmap(initialBmp, initialBmp.getWidth(), initialBmp.getHeight(), false); 
				BitmapDrawable initialCur = new BitmapDrawable(this.getResources(), initialBmp);
				initialCur.setAntiAlias(false);
				toAdd.getLayoutParams().height = initialBmp.getHeight();
				toAdd.getLayoutParams().width = initialBmp.getWidth();
				toAdd.setImageDrawable(initialCur);

				toAdd.setOnDragListener(new View.OnDragListener() {
					@Override
					public boolean onDrag(View v, DragEvent event) {
						if(!isShowingAI) {
							switch (event.getAction()) {
							case DragEvent.ACTION_DRAG_STARTED: break;
							case DragEvent.ACTION_DRAG_ENTERED:
								if(v.isClickable()) {
									/*LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
									lp.setMargins(1, 1, 1, 1);
									v.setLayoutParams(lp);*/
									v.setPadding(1, 1, 1, 1);
									v.setBackgroundColor(Color.WHITE);
								}
								break;
							case DragEvent.ACTION_DRAG_EXITED:
								/*LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
								lp.setMargins(0, 0, 0, 0);
								v.setLayoutParams(lp);*/
								v.setPadding(0,0,0,0);
								v.setBackgroundColor(Color.TRANSPARENT);
								break;
							case DragEvent.ACTION_DROP:
								//ImageView view = (ImageView) event.getLocalState();
								//view.setVisibility(View.INVISIBLE);
								v.setPadding(0,0,0,0);
								v.setBackgroundColor(Color.TRANSPARENT);
								if(v.isClickable()) {
									onClick(v);
									return true;
								} else {
									return false;
								}
							case DragEvent.ACTION_DRAG_ENDED: break;
							default: break;
							}
						}

						return true;
					}
				});
				counter++;
			}
		}

		final Resources res = this.getResources();
		// Get resource for the deckView, then pop the first card off of the stack and set the top of the deckView equal to the cards resource in /res
		deckView 		= (ImageView) findViewById(R.id.deckView);
		deckView.setOnDragListener(new View.OnDragListener() {
			ImageView deckView;
			Drawable backupView;

			@Override
			public boolean onDrag(View v, DragEvent event) {
				if(!isShowingAI) {
					switch (event.getAction()) {
					case DragEvent.ACTION_DRAG_STARTED:
						deckView = (ImageView) event.getLocalState();
						backupView = deckView.getDrawable().getConstantState().newDrawable();
						Bitmap initialBmp = BitmapFactory.decodeResource(res, R.drawable.toprbvert);
						initialBmp = Bitmap.createScaledBitmap(initialBmp, initialBmp.getWidth(), initialBmp.getHeight(), false); 
						final BitmapDrawable initialCur = new BitmapDrawable(res, initialBmp);
						initialCur.setAntiAlias(false);
						deckView.getLayoutParams().height = initialBmp.getHeight();
						deckView.getLayoutParams().width = initialBmp.getWidth();
						deckView.setImageDrawable(initialCur);
						break;
					case DragEvent.ACTION_DRAG_ENTERED:
						break;
					case DragEvent.ACTION_DRAG_EXITED:
						break;
					case DragEvent.ACTION_DROP:
						if(v.isClickable()) { return true; } 
						else {
							deckView.setImageDrawable(backupView);
							return false;
						}
					case DragEvent.ACTION_DRAG_ENDED:
						if(!event.getResult()) {
							System.out.println("The event returned false ");
							deckView.setImageDrawable(backupView);
						}
						break;
					default: break;
					}
				}
				return true;
			}

		});

		deckView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
					v.startDrag(null, shadowBuilder, v, 0);
					v.performClick();
					return true;
				} else { return false; }
			}
		});

		currentDeckCard = deck.pop();
		String fileName = currentDeckCard.toString();
		fileName		= fileName.toLowerCase(Locale.getDefault());
		int resourceID  = getResources().getIdentifier(fileName, "drawable", getPackageName());
		Bitmap deckBmp = BitmapFactory.decodeResource(this.getResources(), resourceID);
		deckBmp = Bitmap.createScaledBitmap(deckBmp, deckBmp.getWidth(), deckBmp.getHeight(), false); 
		BitmapDrawable deckCur = new BitmapDrawable(this.getResources(), deckBmp);
		deckCur.setAntiAlias(false);

		deckView.getLayoutParams().height = deckBmp.getHeight();
		deckView.getLayoutParams().width = deckBmp.getWidth();
		deckView.setImageDrawable(deckCur);

	}

	/**
	 * The method that is called each time any ImageView is pressed in the program. 
	 */
	@Override
	public void onClick(View v) {
		ImageView currentView = map.get(getResources().getResourceEntryName(v.getId()));
		if(currentView.isClickable()) {
			// increment computer move by one
			computer.nextMove();
			computerScore = computer.getScore();
			System.out.println("COMPUTER SCORE: " + computerScore);
			// play simple sound when placing card on the table. short and succinct
			playPlace();
			String fileName 	  = currentDeckCard.toString();
			fileName 			  = fileName.toLowerCase(Locale.getDefault());
			int resourceID 	      = getResources().getIdentifier(fileName, "drawable", getPackageName());

			// Get the coordinates of the view from the name, then add it to the master array of cards for computation purposes
			String imageViewName  = getResources().getResourceEntryName(v.getId());
			int row 			  = Integer.parseInt(imageViewName.substring(1,2)) - 1;
			int col 			  = Integer.parseInt(imageViewName.substring(3,4)) - 1;
			array[row][col]		  = currentDeckCard;

			// since we are using Bitmap playing cards (thanks Susan Kare), 
			//    we have to ensure that they will NOT be anti-aliased
			Bitmap gridBmp = BitmapFactory.decodeResource(this.getResources(), resourceID);
			gridBmp = Bitmap.createScaledBitmap(gridBmp, gridBmp.getWidth(), gridBmp.getHeight(), false); 
			BitmapDrawable bdCur = new BitmapDrawable(this.getResources(), gridBmp);
			bdCur.setAntiAlias(false);
			currentView.getLayoutParams().height = gridBmp.getHeight();
			currentView.getLayoutParams().width = gridBmp.getWidth();
			currentView.setImageDrawable(bdCur);
			currentView.setClickable(false);
			//currentView.setImageBitmap(gridBmp);
			//currentView.setImageResource(resourceID);

			// Get the next card in the deck and make it the next card in the deckView
			currentDeckCard 	  = deck.pop();
			fileName 			  = currentDeckCard.toString();
			fileName 			  = fileName.toLowerCase(Locale.getDefault());
			resourceID  		  = getResources().getIdentifier(fileName, "drawable", getPackageName());

			Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), resourceID);
			bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), false); 
			BitmapDrawable bd = new BitmapDrawable(this.getResources(), bmp);
			bd.setAntiAlias(false);
			deckView.getLayoutParams().height = bmp.getHeight();
			deckView.getLayoutParams().width = bmp.getWidth();
			deckView.setImageDrawable(bd);

			moves++;
			updateArray();
			updateComputerArray();
			checkScoreUpdateLabels(places);
			updateTotal();

			// end the game
			if(moves==25) { endGame(); }
		}
	}

	/**
	 * Uses a temporary List to gather elements from the master array[][]  
	 * Puts List into the master ArrayList of Lists in order for computation purposes for the scoring
	 */
	private void updateArray(){
		List<Card> tmp = new LinkedList<Card>();
		places = new ArrayList<List<Card>>(); 

		// Get [rows][cols]
		for (int r=0; r<array.length; r++) {
			for(int c=0; c<array[r].length; c++) {
				if(array[r][c]!=null) { // If the value exists, add it to a temporary list for the row/col
					tmp.add(array[r][c]);
				}
			}
			places.add(tmp);
			tmp = new LinkedList<Card>();
		}

		tmp = new LinkedList<Card>();

		// Get [cols][rows]
		for (int r=0; r<array.length; r++) {
			for(int c=0; c<array[r].length; c++) {
				if(array[c][r]!=null) {
					tmp.add(array[c][r]);
				}
			}
			places.add(tmp);
			tmp = new LinkedList<Card>();
		}

		// Sort all of the temporary lists (since our Card class implements Comparable)
		for (int i=0; i<places.size(); i++) {
			Collections.sort(places.get(i));
		}

		/* 
		 * // DEBUG: Output the text representation of the sorted card arrays. For debug purposes -- Delete before final release.
		for(int i=0; i<places.size(); i++) {
			for(int k=0; k<places.get(i).size(); k++) {
				System.out.print(places.get(i).get(k) + " ");
			}
			System.out.println();
		}
		 */
	}

	public void updateComputerArray() {
		List<Card> tmp = new LinkedList<Card>();
		computerPlaces = new ArrayList<List<Card>>(); 

		// Get [rows][cols]
		for (int r=0; r<computer.getGrid().length; r++) {
			for(int c=0; c<computer.getGrid()[r].length; c++) {
				if(computer.getGrid()[r][c] != null) {
					// If the value exists, add it to a temporary list for the row/col
					edu.gettysburg.ai.Card ca = computer.getGrid()[r][c];
					// reverse the interpret
					int newRank = ca.getRank()-1;
					if(ca.getRank()==0)
						newRank = 12;

					// http://stackoverflow.com/questions/609860/convert-from-enum-ordinal-to-enum-type
					Card pca= new Card(Card.Rank.values()[newRank], Card.Suit.values()[ca.getSuit()]);
					tmp.add(pca);
				}
			}
			computerPlaces.add(tmp);
			tmp = new LinkedList<Card>();
		}
		tmp = new LinkedList<Card>();

		// Get [cols][rows]
		for (int r=0; r<computer.getGrid().length; r++) {
			for(int c=0; c<computer.getGrid()[r].length; c++) {
				if(computer.getGrid()[c][r] != null) {
					// If the value exists, add it to a temporary list for the row/col
					edu.gettysburg.ai.Card ca = computer.getGrid()[c][r];
					// reverse the interpret
					int newRank = ca.getRank()-1;
					if(ca.getRank()==0)
						newRank = 12;

					// http://stackoverflow.com/questions/609860/convert-from-enum-ordinal-to-enum-type
					Card pca= new Card(Card.Rank.values()[newRank], Card.Suit.values()[ca.getSuit()]);
					tmp.add(pca);
				}
			}
			computerPlaces.add(tmp);
			tmp = new LinkedList<Card>();
		}

		// Sort all of the temporary lists (since our Card class implements Comparable)
		for (int i=0; i<computerPlaces.size(); i++) {
			Collections.sort(computerPlaces.get(i));
		}
	}

	/**
	 * When the game ends, update the array and check the score one final time and then update the total.
	 * Then show an alertDialog allowing the user to either continue playing or exit the game.
	 */
	public void endGame() {
		updateArray();
		checkScoreUpdateLabels(places);
		deckView.setImageResource(getResources().getIdentifier("nblank", "drawable", getPackageName()));
		updateTotal();
		showAI();
		checkScoreUpdateLabels(computerPlaces);
		updateComputerTotal();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder       
		.setCancelable(false)
		.setPositiveButton("New Game", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// Finish the current Intent, and start over
				Intent intent = getIntent();
				killAI();
				finish();
				startActivity(intent);
			}
		})
		.setNegativeButton("Quit", new DialogInterface.OnClickListener() {           
			public void onClick(DialogInterface dialog, int id) {                
				// Proper way of ending Intent
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				MainActivity.this.finish();
			}
		});

		if(computerScore > gameTotal) {
			builder.setMessage("Game Over! \n" + userName + ", you lose!" + " \nYour score was " + gameTotal 
					+ "\n" + "Computer score was " + computerScore);
			losses++;
		} else if(computerScore == gameTotal) {
			builder.setMessage("Game Over! \n" + userName + ", you tied!" + " \nYour score was " + gameTotal 
					+ "\n" + "Computer score was " + computerScore);
			ties++;
		} else {
			builder.setMessage("Game Over! \n" + userName + ", you win!" + " \nYour score was " + gameTotal
					+ "\n" + "Computer score was " + computerScore);
			wins++;
		}

		if(gameTotal > highestScore) {
			highestScore = gameTotal;
		}

		String FILENAME = userName+".txt";
		FileOutputStream fos = null;

		try {
			fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
			writer.write(String.valueOf(wins));
			writer.newLine();
			writer.write(String.valueOf(losses));
			writer.newLine();
			writer.write(String.valueOf(ties));
			writer.newLine();
			writer.write(String.valueOf(highestScore));
			writer.newLine();
			writer.write(String.valueOf(isMuted));
			writer.flush();
			fos.close();
		} catch (IOException e) { e.printStackTrace(); }

		AlertDialog alert = builder.create();
		Window window = alert.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();

		wlp.gravity = Gravity.BOTTOM;
		wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		window.setAttributes(wlp);
		alert.show();
	}

	private void killAI() {
		stopService(new Intent(this, bgService.class));
	}

	/**
	 * Play the place sound when a user clicks on a click-able ImageView. 
	 * For Reference: Sound defined in res/raw/cardplace.wav
	 */
	private void playPlace() {
		new Thread(new Runnable() {
			public void run() {
				Thread.yield();
				mp = MediaPlayer.create(MainActivity.this, R.raw.cardplace);
				if(mp == null) {            
					System.out.println("Create() on MediaPlayer failed.");       
				} else if(!isMuted) {
					mp.setOnCompletionListener(new OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mediaplayer) {
							mediaplayer.stop();
							mediaplayer.release();

						}});
					mp.start();
				}}}).start();
	}

	/**
	 * Check the scoring value of each of the Lists in the places ArrayList instance variable. 
	 * Could easily implement a British scoring system option in the future...
	 */
	public void checkScoreUpdateLabels(ArrayList<List<Card>> l) { // places and computerPlaces
		for(int i=0; i<l.size(); i++) {
			List<Card> tmp = l.get(i);
			int sectionTotal = 0;
			//boolean scored = false;

			if(tmp.size()==5 && Card.isRoyalFlush(tmp)) {
				sectionTotal+=100;
			}
			else if(tmp.size()==5 && Card.isStraightFlush(tmp)) {
				sectionTotal+=75;
			}
			else if(tmp.size()>=4 && Card.isFourOfAKind(tmp)) {
				sectionTotal+=50;
			}
			else if(tmp.size()==5 && Card.isFullHouse(tmp)) {
				sectionTotal+=25;
			}
			else if(tmp.size()==5 && Card.isFlush(tmp)) {
				sectionTotal+=20;
			}
			else if(tmp.size()==5 && Card.isStraight(tmp)) {
				sectionTotal+=15;
			}
			else if(tmp.size()>=3 && Card.hasThreeOfAKind(tmp)) {
				sectionTotal+=10;
			}
			else if(tmp.size()>=4 && Card.hasTwoPair(tmp, tmp.size())) {
				sectionTotal+=5;
			}
			else if(tmp.size()>=2 && Card.hasPair(tmp)) {
				sectionTotal+=2;
			}
			/*if(sectionTotal > 0) { scored = true; }
			if(scored) {
				for(Card c: tmp) {
					Iterator it = map.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry)it.next();
						//System.out.println(pair.getKey() + " = " + pair.getValue());
						ImageView img = (ImageView) pair.getValue();

						it.remove(); // avoids a ConcurrentModificationException
					}
					System.out.println(c.toString());
				}
			}
			 */
			textMap.get("view"+ i).setText(String.valueOf(sectionTotal));
		}
	}

	/**
	 * Scrape the values of each of the views and add them all together for the total.
	 */
	public void updateTotal() {
		int total = 0;
		for(int i=0; i<places.size(); i++) {
			total+=Integer.parseInt(String.valueOf(textMap.get("view"+ i).getText()));
		}
		gameTotal = total;
		textTotal.setText(String.valueOf(gameTotal));
	}

	public void updateComputerTotal() {
		textTotal.setText(String.valueOf(computerScore));
	}

	/**
	 * When the user presses the physical back button on their device, properly finish the Intent and exit the application
	 */
	@Override
	public void onBackPressed() {
		String FILENAME = userName+".txt";
		FileOutputStream fos = null;

		try {
			fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
			writer.write(String.valueOf(wins));
			writer.newLine();
			writer.write(String.valueOf(losses));
			writer.newLine();
			writer.write(String.valueOf(ties));
			writer.newLine();
			writer.write(String.valueOf(highestScore));
			writer.newLine();
			writer.write(String.valueOf(isMuted));
			writer.flush();
			fos.close();
		} catch (IOException e) { e.printStackTrace(); }
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		killAI();
		finish();
	}

	/**
	 * Save instance variables for when the application is tilted to either landscape or portrait mode 
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	/**
	 * Restore instance variables for when the application is tilted to either landscape or portrait mode 
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * Method to determine functionality of the menu items when they are pressed. 
	 * For Reference: Menu items defined in res/menu/my_options_menu.xml
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.show_AI:
			if(isAllowedToShow) {
				if(isShowingAI) {
					removeAI();
					item.setTitle("Show AI");
					checkScoreUpdateLabels(places);
					updateTotal();
				}
				else {
					showAI();
					item.setTitle("Hide AI");
					checkScoreUpdateLabels(computerPlaces);
					updateComputerTotal();
				}
				isShowingAI = !isShowingAI;
			}
			return true;
		case R.id.about:
			Toast.makeText(getApplicationContext(), 
					"Created by John D. Duncan, III as a "
							+ "Gettysburg College Association for "
							+ "Computing Machinery undergrad project.\n"
							+ "AI Player created by Dr. Todd W. Neller of "
							+ "the Gettysburg College Computer Science faculty.",
							Toast.LENGTH_LONG).show();
			return true;
		case R.id.stats:
			Toast.makeText(getApplicationContext(), 
					"Wins: " + wins + "\n" + 
							"Losses: " + losses + "\n" + 
							"Ties: " + ties + "\n" +
							"Win Average: " + roundTD((double)((double)wins / (wins+losses+ties))*100) + "%\n" +
							"Highest Score: " + highestScore,
							Toast.LENGTH_LONG).show();
			return true;
		case R.id.mute:
			if(isMuted) {
				Toast.makeText(getApplicationContext(), "Sound Un-Muted",
						Toast.LENGTH_SHORT).show();
				item.setTitle("Mute");
				isMuted=!isMuted;
			} else {
				Toast.makeText(getApplicationContext(), "Sound Muted",
						Toast.LENGTH_SHORT).show();
				item.setTitle("Un-Mute");
				isMuted=!isMuted;
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.my_options_menu, menu);
		super.onCreateOptionsMenu(menu);
		this.menu = menu;

		if(isMuted){
			menu.findItem(R.id.mute).setTitle("Un-mute");
		} else {
			menu.findItem(R.id.mute).setTitle("Mute");
		}
		
		if(isShowingAI) {
			menu.findItem(R.id.show_AI).setTitle("Hide AI");
		} else {
			menu.findItem(R.id.show_AI).setTitle("Show AI");
		}

		return true;
	}

	public void showAI() {
		edu.gettysburg.ai.Card[][] grid = computer.getGrid();
		/*for (int j = 0; j<grid[0].length; j++){
			for (int i = 0; i<grid.length; i++){
				System.out.println(grid[j][i]);
			}
		}*/
		isAllowedToShow = false;
		int internalCounter = 0;
		int counter = 0;
		long delay = 0;
		// so we can initialize the card faces with a cool pattern
		// Get resources for all of the ImageViews, set their onClickListener, 
		//    and then add them to them hashMap of ImageViews using its ID as the key
		for(int col=1; col<6; col++) {
			internalCounter = 0;
			for(int row=1; row<6; row++) {
				int resourceID 	= getResources().getIdentifier("r" + row + "c" + col, "id", getPackageName());
				final ImageView toAdd = (ImageView) findViewById(resourceID);
				//toAdd.setOnClickListener(null);
				toAdd.setClickable(false);
				computerMap.put("r" + row + "c" + col, toAdd);
				Bitmap initialBmp = null;

				if(grid[internalCounter][counter] == null) {
					initialBmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.toprbvert);
				} else {
					edu.gettysburg.ai.Card ca = grid[internalCounter][counter];
					// reverse the interpret
					int newRank = ca.getRank()-1;
					if(ca.getRank()==0) { newRank = 12; }

					// http://stackoverflow.com/questions/609860/convert-from-enum-ordinal-to-enum-type
					Card pca= new Card(Card.Rank.values()[newRank], Card.Suit.values()[ca.getSuit()]);
					String fileName 	  = pca.toString();
					fileName 			  = fileName.toLowerCase(Locale.getDefault());
					int nResourceID 	      = getResources().getIdentifier(fileName, "drawable", getPackageName());
					// Get the coordinates of the view from the name, then add it to the master array of cards for computation purposes
					initialBmp = BitmapFactory.decodeResource(this.getResources(), nResourceID);
				}

				initialBmp = Bitmap.createScaledBitmap(initialBmp, initialBmp.getWidth(), initialBmp.getHeight(), false); 
				final BitmapDrawable initialCur = new BitmapDrawable(this.getResources(), initialBmp);
				initialCur.setAntiAlias(false);
				toAdd.getLayoutParams().height = initialBmp.getHeight();
				toAdd.getLayoutParams().width = initialBmp.getWidth();

				// http://stackoverflow.com/questions/7785649/creating-a-3d-flip-animation-in-android-using-xml
				final ObjectAnimator anim = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.flipping); 
				final int c = col;

				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {
						anim.setTarget(toAdd);
						anim.setDuration(1500);
						anim.start();

						new Thread(new Runnable() {
							public void run() {
								//Thread.yield();
								try { Thread.sleep((1500/2) - 250); } 
								catch (InterruptedException e) { e.printStackTrace(); }

								runOnUiThread(new Runnable() {
									public void run() {
										// http://stackoverflow.com/questions/7785649/creating-a-3d-flip-animation-in-android-using-xml
										toAdd.setImageDrawable(initialCur);
										if(c == 5) { isAllowedToShow = true; }
									}
								});
							}
						}).start();
					}
				}, delay);
				if(row % 5 == 0) { delay += 150; }
				internalCounter++;
			}
			counter++;
		}
	}

	public void removeAI(){
		// so we can initialize the card faces with a cool pattern
		// Get resources for all of the ImageViews, set their onClickListener, 
		//    and then add them to them hashMap of ImageViews using its ID as the key
		isAllowedToShow = false;
		int internalCounter = 0;
		int counter = 4;
		int cardFaceCounter = 2;
		long delay = 0;

		for(int col=5; col>0; col--) { // this is for the actual card imagviews
			internalCounter = 0;
			for(int row=1; row<6; row++) {
				int resourceID 	= getResources().getIdentifier("r" + row + "c" + col, "id", getPackageName());
				final ImageView toAdd = (ImageView) findViewById(resourceID);
				String fileName="";
				Bitmap initialBmp = null;

				// if there is a card placed there..
				if(array[internalCounter][counter] != null){
					isAllowedToPress = false;
					// get the toString (which should let us grab the file)
					fileName = array[internalCounter][counter].toString();
					fileName		= fileName.toLowerCase(Locale.getDefault());
					// grab the file (for example, deucespades.png)
					int nResourceID  = getResources().getIdentifier(fileName, "drawable", getPackageName());
					initialBmp = BitmapFactory.decodeResource(this.getResources(), nResourceID);
				}
				else {
					isAllowedToPress = true;
					// dope ternary - you = jealous (re-do the user pattern)
					initialBmp = cardFaceCounter % 2 == 0 ? BitmapFactory.decodeResource(this.getResources(), R.drawable.topbvert): 
						BitmapFactory.decodeResource(this.getResources(), R.drawable.toprvert);
					// only allow those that have not been selected already to be clicked
				}

				// create the scaled bitmap (do not want phone-specific scaling)
				initialBmp = Bitmap.createScaledBitmap(initialBmp, initialBmp.getWidth(), initialBmp.getHeight(), false); 
				final BitmapDrawable initialCur = new BitmapDrawable(this.getResources(), initialBmp);
				initialCur.setAntiAlias(false);
				toAdd.getLayoutParams().height = initialBmp.getHeight();
				toAdd.getLayoutParams().width = initialBmp.getWidth();

				// http://stackoverflow.com/questions/7785649/creating-a-3d-flip-animation-in-android-using-xml
				final ObjectAnimator anim = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.flipping_back); 
				Handler handler = new Handler();
				// isAllowedToPress gets reset to its original value below. copy into final 
				final boolean isAllowedToPressLocal = isAllowedToPress;
				final int c = col;

				handler.postDelayed(new Runnable() {
					public void run() {
						anim.setTarget(toAdd);
						anim.setDuration(1500);
						anim.start();

						new Thread(new Runnable() {
							public void run() {
								//Thread.yield();
								try { Thread.sleep((1500/2) - 250); } 
								catch (InterruptedException e) { e.printStackTrace(); }

								runOnUiThread(new Runnable() {
									public void run() {
										// http://stackoverflow.com/questions/7785649/creating-a-3d-flip-animation-in-android-using-xml
										toAdd.setImageDrawable(initialCur);

										if(isAllowedToPressLocal){ toAdd.setClickable(true); }
										if(c == 1) { isAllowedToShow = true; }
									}
								});
							}
						}).start();
					}
				}, delay);
				if(row % 5 == 0) { delay += 150; }
				internalCounter++; // we want to increase rows
				cardFaceCounter++;
			}
			counter--; // decrease cols
		}
	}

	/* Touch-related debug stuff */
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			// This happens when you touch the screen with two fingers
			mode = SWIPE;
			// You can also use event.getY(1) or the average of the two
			startY = event.getY(0);
			startX = event.getX(0);
			break;
		case MotionEvent.ACTION_POINTER_UP:
			// This happens when you release the second finger
			mode = NONE;
			if((Math.abs(startY - stopY) > TRESHOLD) ) {
				if(startY > stopY) {
					System.out.println("SWIPING UP");
				}
				else {
					System.out.println("SWIPING DOWN");
				}

			}
			if((Math.abs(startX - stopX) > TRESHOLD)) {
				if(isAllowedToShow) {
					MenuItem showAIButton = null;
					if(menu != null) {
						showAIButton = menu.findItem(R.id.show_AI);
					}
					
					if((startX < stopX) && !isShowingAI) {
						showAI(); 
						if(showAIButton != null) {
							showAIButton.setTitle("Hide AI");
						}
						checkScoreUpdateLabels(computerPlaces);
						updateComputerTotal();
						isShowingAI = true;
					} else if((startX > stopX) && isShowingAI){
						removeAI();
						if(showAIButton != null) {
							showAIButton.setTitle("Show AI");
						}
						checkScoreUpdateLabels(places);
						updateTotal();
						isShowingAI = false;
					}
				}
			}

			this.mode = NONE;
			v.performClick();
			break;
		case MotionEvent.ACTION_MOVE:
			if(mode == SWIPE) {
				stopY = event.getY(0);
				stopX = event.getX(0);
			}
			break;
		}
		return true;
	}

	private void copyAssets() {
		AssetManager assetManager = getAssets();
		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open("narl.dat");
			out = openFileOutput("narl.dat", Context.MODE_PRIVATE);
			//out = new FileOutputStream(outFile);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch(IOException e) {
			Log.e("tag", "Failed to copy asset file: " + "narl.dat", e);
		}       

	}
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}
	public double roundTD(double d) {
		d = Math.round(d * 100);
		d = d/100;
		return d;
	}
}
import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
//ADDED NEW CODE
import java.net.*;
import javax.swing.event.*;

public class BeatBoxFinal
{
	JFrame theFrame;
	JPanel mainPanel;
	JList<String> incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkboxList;
	int nextNum;
	Vector<String> listVector = new Vector<String>();	//new thing
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();	//new thing

	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;

	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat","Acoustic Snare","Crash Cymbal","Hand Clap","High Tom","Hi Bongo","Maracas","Whistle","Low Conga","Cowbell","Vibraslap","Low-mid Tom","High Agogo","Open Hi Conga"};
	int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
	
	public static void main(String args[])
	{	
		if(args.length == 0)
		{
			System.out.println("Please run the application with your Username defined.");
			System.exit(0);
		}
		new BeatBoxFinal().startUp(args[0]);	//args[0] is your user ID/screen name
	}
	
	public void startUp(String name)
	{
		userName = name;
		//open connection to the server
		try
		{
			Socket sock = new Socket("localhost", 4242);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader());
			remote.start();
		}
		catch(Exception ex)
		{
			System.out.println("couldn't connect - you'll have to play alone.");
		}
	
		setUpMidi();
		buildGUI();
	}
	@SuppressWarnings("unchecked")
	public void buildGUI()
	{
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout() ;
		JPanel background = new JPanel(layout) ;
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

		checkboxList  = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		
		JButton rand = new JButton("Randomize");
		rand.addActionListener(new MyRandListener());
		buttonBox.add(rand);
		
		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);

		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);

		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);

		JButton load = new JButton("Load track");
		load.addActionListener(new MyReadInListener());
		buttonBox.add(load);
		
		JButton sendIt = new JButton("Send It");
		sendIt.addActionListener(new MySendListener());
		buttonBox.add(sendIt);

		userMessage = new JTextField();
		buttonBox.add(userMessage);

		//ADDED NEW CODE
		incomingList = new JList<String>();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);	//new thing
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector);	//no data to start with	//new thing

		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i = 0;i<16;i++)
			nameBox.add(new Label(instrumentNames[i]));
	
		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		
		theFrame.getContentPane().add(background);
		GridLayout grid = new GridLayout(16,16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);

		for(int i=0;i<256;i++)
		{
			JCheckBox c = new JCheckBox();
			c.setSelected(false);	
			checkboxList.add(c);
			mainPanel.add(c);
		}

		theFrame.setBounds(50,50,400,400);
		theFrame.pack();	//--pg.no.430 using pack()
		theFrame.setVisible(true);
	}

	public void setUpMidi()
	{
		try
		{
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ,4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		}
		catch(Exception e)
		{e.printStackTrace();}
	}

	public void buildTrackAndStart()
	{
		ArrayList<Integer> trackList = null;	//now we use ArrayList<Integer> instead of int[];
		
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i=0;i<16;i++)
		{
			trackList = new ArrayList<Integer>();
			
			for(int j=0;j<16;j++)
			{
				JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
				if(jc.isSelected())
				{
					int key = instruments[i];
					trackList.add(key);
				}
				
				else
					trackList.add(null);	//now we use null instead of '0'
			}

			makeTracks(trackList);
			//track.add(makeEvent(176,1,127,0,16)); removing this
		}

		track.add(makeEvent(192,9,1,0,15));	// - so we always go to full 16 beats
		try
		{
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);	
			sequencer.start();
			sequencer.setTempoInBPM(120);
		}
		catch(Exception e)
		{e.printStackTrace();}
	}

	//----------------------------------------------------------------------------------------------------

	public class MyStartListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			buildTrackAndStart();
		}
	}

	public class MyStopListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			sequencer.stop();
		}
	}

	public class MyUpTempoListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			float tempoFactor = sequencer.getTempoFactor();	
			sequencer.setTempoFactor((float)(tempoFactor * 1.03));
		}
	}

	public class MyDownTempoListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			float tempoFactor = sequencer.getTempoFactor();	
			sequencer.setTempoFactor((float)(tempoFactor * 0.97));;
		}
	}

	public class MyRandListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			
		}
	}

	//----------server chat----------------------------------------------------------------------------
	//ADDED NEW CODE
	
	public class MySendListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			//make an arraylist of just the STATE of the checkbox
			boolean[] checkboxState = new boolean[256];
			for(int i = 0; i < 256; i++)
			{
				JCheckBox check =(JCheckBox) checkboxList.get(i);
				if(check.isSelected())
					checkboxState[i] = true;
			}
			String messageToSend = null;
			
			try
			{
				out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
				out.writeObject(checkboxState);
			}
			catch(Exception ex)
			{
				System.out.println("Sorry dude. Could not send it to the server.");
			}
			userMessage.setText(""); //reset
		}
	}
	
	//ADDED NEW CODE- imp* (SAVING/LOADING file DATA)
	public class MyListSelectionListener implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent le)	//new thing
		{
			if(!le.getValueIsAdjusting())
			{
				String selected = (String) incomingList.getSelectedValue();
				if(selected != null)
				{
					//now got to the map, and change the sequence
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					
					boolean[] checkboxState = new boolean[256];
					for(int i = 0; i < 256; i++)
					{
						JCheckBox check =(JCheckBox) checkboxList.get(i);
						if(check.isSelected())
							checkboxState[i] = true;
					}
					sequencer.stop();
						
					if(Arrays.equals(selectedState,checkboxState))	//check if new displaying Jlist item is same track as current set
						changeSequence(selectedState);
					else						//if not same, SAVE the current track, & load JList item track!!!
					{
						JFileChooser fileSave = new JFileChooser();
						fileSave.showSaveDialog(theFrame);
						File f = fileSave.getSelectedFile();
			
						try
						{
							FileOutputStream fileStream = new FileOutputStream(f);
							ObjectOutputStream os = new ObjectOutputStream(fileStream);
							os.writeObject(checkboxState);
						}
						catch(Exception ex)
							{ex.printStackTrace();}
						
						changeSequence(selectedState);
					}
					
					buildTrackAndStart();
					incomingList.clearSelection();	//clears selection made in list;
				}
			}
		}
	}

	public class MyReadInListener implements ActionListener	//LOAD FILE
	{
		public void actionPerformed(ActionEvent ev)
		{
			boolean[] checkboxState = null;

			JFileChooser fileOpen = new JFileChooser();
			fileOpen.showOpenDialog(theFrame);
			File f = fileOpen.getSelectedFile();
			try
			{
				FileInputStream fileIn = new FileInputStream(f);
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkboxState = (boolean[]) is.readObject();
			}
			catch(Exception ex)
			{ex.printStackTrace();}
		
			for(int i=0; i<256;i++)
			{
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(checkboxState[i])
					check.setSelected(true);	//new thing
				else
					check.setSelected(false);
			}
			
			sequencer.stop();
			buildTrackAndStart();
		}
	}	
	
	//----------------thread implementation-------------------------------------------------------------
	//ADDED NEW CODE

	//@SuppressWarnings("unchecked") no need
	public class RemoteReader implements Runnable
	{
		boolean[] checkboxState = null;
		String nameToShow = null;
		Object obj = null;
	
		public void run()
		{
			try
			{
				while((obj=in.readObject()) != null)
				{
					System.out.println("got an object from server");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;	//new thing
					checkboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkboxState);	//new thing
					listVector.add(nameToShow);
					incomingList.setListData(listVector);	//java uses uncheck/unsafe operation..PROBLEM here
				}                                               //Solution: JList<String> incomingList = new JList<>(); Define it whats going in..not generic type
			}							// 	    As 'listVector' has <String>.
			catch(Exception ex)
			{ex.printStackTrace();}
		}
	}

	//-----------------------------------------------------------------------------------------------
	//ADDED NEW CODE
	
	public class MyPlayMineListener implements ActionListener	//NOT USING
	{
		public void actionPerformed(ActionEvent a)
		{
			if(mySequence != null)
				sequence = mySequence;	//restore to my original
		}
	}
	
	public void changeSequence(boolean[] checkboxState)
	{
		for(int i = 0; i<256; i++)
		{
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if(checkboxState[i])
				check.setSelected(true);
			else
				check.setSelected(false);
		}
	}

	//--------------old code------------------------------------------------------------------------------
	
	public void makeTracks(ArrayList list)	//using ArrayList instead of int[]
	{
		Iterator it= list.iterator();	//new thing
		for(int i=0; i<16; i++)
		{
			Integer num = (Integer) it.next();
			
			if(num != null)
			{
				int numKey = num.intValue();	//new thing		
				track.add(makeEvent(144,9,numKey,100,i));
				track.add(makeEvent(128,9,numKey,100,i+1));
			}
		}
	}
	
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick)
	{
		MidiEvent event = null;
		try
		{
			ShortMessage a= new ShortMessage();
			a.setMessage(comd,chan,one,two);
			event = new MidiEvent(a,tick);
		}
		catch(Exception e)
		{e.printStackTrace();}

		return event;
	}

	
}
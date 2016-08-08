/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/

package chat.client;

//#MIDP_EXCLUDE_FILE

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import chat.client.agent.ChatClientAgent;
import chat.mobile.LocationTableModel;
import chat.mobile.MobileAgent;
import jade.core.Location;
import jade.gui.GuiEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;

/**
   @author Giovanni Caire - TILAB
 */
public class AWTChatGui extends JFrame implements ChatGui {
	private ChatClientAgent myAgent;
	private MobileAgent myAgent2;
	private TextField writeTf;
	private TextArea allTa;
	private ParticipantsFrame participantsFrame;
	private Button b_1;
	
	private LocationTableModel visitedSiteListModel;
	private JTable            visitedSiteList;
	private LocationTableModel availableSiteListModel;
	private JTable            availableSiteList;
	private JTextField counterText; 
	
	
	private static String MOVELABEL = "MOVE";
	private static String CLONELABEL = "CLONE";
	private static String EXITLABEL = "EXIT";
	private static String PAUSELABEL = "Stop Counter";
	private static String CONTINUELABEL = "Continue Counter";
	private static String REFRESHLABEL = "Refresh Locations";
	
	public AWTChatGui(ChatClientAgent a) {
		myAgent = a;
		setTitle("Chat: "+myAgent.getLocalName());
		setSize(new Dimension(538, 517));
		Panel p = new Panel();
		p.setBounds(0, 0, 270, 22);
		p.setLayout(new BorderLayout());
		writeTf = new TextField();
		p.add(writeTf, BorderLayout.CENTER);
		Button b = new Button("Send");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		  	String s = writeTf.getText();
		  	if (s != null && !s.equals("")) {
			  	myAgent.handleSpoken(s);
			  	writeTf.setText("");
		  	}
			} 
		} );
		getContentPane().setLayout(null);
		p.add(b, BorderLayout.EAST);
		getContentPane().add(p);
		
		
		////////////////////////////////
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main,BoxLayout.Y_AXIS));

		JPanel counterPanel = new JPanel();
		counterPanel.setLayout(new BoxLayout(counterPanel, BoxLayout.X_AXIS));
		
		JButton pauseButton = new JButton("STOP COUNTER");
		pauseButton.addActionListener((ActionListener) this);
		JButton continueButton = new JButton("CONTINUE COUNTER");
		continueButton.addActionListener((ActionListener) this);
		JLabel counterLabel = new JLabel("Counter value: ");
		counterText = new JTextField();
		counterPanel.add(pauseButton);
		counterPanel.add(continueButton);
		counterPanel.add(counterLabel);
		counterPanel.add(counterText);
		
		main.add(counterPanel);
		//////////////////////////////
		
///////////////////////////////////////////////////
// Add the list of available sites to the NORTH part 
availableSiteListModel = new LocationTableModel();
availableSiteList = new JTable(availableSiteListModel);
availableSiteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

JPanel availablePanel = new JPanel();
availablePanel.setLayout(new BorderLayout());

JScrollPane avPane = new JScrollPane();
avPane.getViewport().setView(availableSiteList);
availablePanel.add(avPane, BorderLayout.CENTER);
availablePanel.setBorder(BorderFactory.createTitledBorder("Available Locations"));
availableSiteList.setRowHeight(20);

main.add(availablePanel);

TableColumn c;
c = availableSiteList.getColumn(availableSiteList.getColumnName(0));
c.setHeaderValue("ID");
c = availableSiteList.getColumn(availableSiteList.getColumnName(1));
c.setHeaderValue("Name");
c = availableSiteList.getColumn(availableSiteList.getColumnName(2));
c.setHeaderValue("Protocol");
c = availableSiteList.getColumn(availableSiteList.getColumnName(3));
c.setHeaderValue("Address");

///////////////////////////////////////////////////
// Add the list of visited sites to the CENTER part 
JPanel visitedPanel = new JPanel();
visitedPanel.setLayout(new BorderLayout());
visitedSiteListModel = new LocationTableModel();
visitedSiteList = new JTable(visitedSiteListModel);
JScrollPane pane = new JScrollPane();
pane.getViewport().setView(visitedSiteList);
visitedPanel.add(pane,BorderLayout.CENTER);
visitedPanel.setBorder(BorderFactory.createTitledBorder("Visited Locations"));
visitedSiteList.setRowHeight(20);

main.add(visitedPanel);

// Column names

c = visitedSiteList.getColumn(visitedSiteList.getColumnName(0));
c.setHeaderValue("ID");
c = visitedSiteList.getColumn(visitedSiteList.getColumnName(1));
c.setHeaderValue("Name");
c = visitedSiteList.getColumn(visitedSiteList.getColumnName(2));
c.setHeaderValue("Protocol");
c = visitedSiteList.getColumn(visitedSiteList.getColumnName(3));
c.setHeaderValue("Address");
		
		
		allTa = new TextArea();
		allTa.setBounds(0, 22, 270, 183);
		allTa.setEditable(false);
		allTa.setBackground(Color.white);
		getContentPane().add(allTa);
		
		b_1 = new Button("Participants");
		b_1.setBounds(0, 205, 138, 40);
		b_1.setPreferredSize(new Dimension(40,40));
		b_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!participantsFrame.isVisible()) {
					participantsFrame.setVisible(true);
				}	
			} 
		} );
		getContentPane().add(b_1);
		

		Button b2 = new Button(MOVELABEL);
		b2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();

				// MOVE
				if      (command.equalsIgnoreCase(MOVELABEL)) {
				  Location dest;
				  int sel = availableSiteList.getSelectedRow();
				  if (sel >= 0)
				    dest = availableSiteListModel.getElementAt(sel);
				  else
				    dest = availableSiteListModel.getElementAt(0);
		              
				  GuiEvent ev = new GuiEvent((Object) this,myAgent2.MOVE_EVENT);
				  ev.addParameter(dest);
		      myAgent2.postGuiEvent(ev);	 
				}
				else if (command.equalsIgnoreCase(PAUSELABEL)) {
		      GuiEvent ev = new GuiEvent(null,myAgent2.STOP_EVENT);
				  myAgent2.postGuiEvent(ev);
				}
				else if (command.equalsIgnoreCase(CONTINUELABEL)) {
				     GuiEvent ev = new GuiEvent(null,myAgent2.CONTINUE_EVENT);
				     myAgent2.postGuiEvent(ev);
				}
				else if (command.equalsIgnoreCase(REFRESHLABEL)) {
				     GuiEvent ev = new GuiEvent(null,myAgent2.REFRESH_EVENT); 
		         myAgent2.postGuiEvent(ev);
				}
			}
		});
		b2.setActionCommand("Mover");
		b2.setBounds(138, 205, 70, 40);
		getContentPane().add(b2);
		
		Button button = new Button(CLONELABEL);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Location dest;
				  int sel = availableSiteList.getSelectedRow();
				  if (sel >= 0)
				    dest = availableSiteListModel.getElementAt(sel);
				  else
				    dest = availableSiteListModel.getElementAt(0);
				  GuiEvent ev = new GuiEvent((Object) this, myAgent2.CLONE_EVENT);
				  ev.addParameter(dest);
		      myAgent2.postGuiEvent(ev);
			}
		});
		button.setBounds(207, 205, 63, 40);
		getContentPane().add(button);
		
		Button button_1 = new Button(EXITLABEL);
		button_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GuiEvent ev = new GuiEvent(null,myAgent.EXIT);
				myAgent2.postGuiEvent(ev);
			}
		});
		button_1.setBounds(0, 245, 270, 46);
		getContentPane().add(button_1);
		
		participantsFrame = new ParticipantsFrame(this, myAgent.getLocalName());
		
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		show();
	}
	
	void displayCounter(int value){
	    counterText.setText(Integer.toString(value));
	    //counterText.fireActionPerformed();
	  }

	  public void updateLocations(Iterator list) {
	    availableSiteListModel.clear();
	    for ( ; list.hasNext(); ) {
	    	Object obj = list.next();
	      availableSiteListModel.add((Location) obj);
	    }
	    availableSiteListModel.fireTableDataChanged();
	  }
	
	public void notifyParticipantsChanged(String[] names) {
		if (participantsFrame != null) {
			participantsFrame.refresh(names);
		}
	}
	
	public void notifySpoken(String speaker, String sentence) {
		allTa.append(speaker+": "+sentence+"\n");
	}
	
	Dimension getProperSize(int maxX, int maxY) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width < maxX ? screenSize.width : maxX);
		int y = (screenSize.height < maxY ? screenSize.height : maxY);
		return new Dimension(x, y);
	}
	
	void showCorrect()
	{
		///////////////////////////////////////////
		// Arrange and display GUI window correctly
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		show();
	}
	
	public void addVisitedSite(Location site)
	{
		visitedSiteListModel.add(site);
		visitedSiteListModel.fireTableDataChanged();

	}
	
	
	
	public void dispose() {
		participantsFrame.dispose();
		super.dispose();
	}
}




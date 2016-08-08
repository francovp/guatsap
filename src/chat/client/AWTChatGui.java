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

import javax.swing.JFrame;

import chat.client.agent.ChatClientAgent;
import chat.mobile.MobileAgent;
import chat.mobile.MobileAgentGui;

import javax.swing.JButton;

/**
   @author Giovanni Caire - TILAB
 */
public class AWTChatGui extends JFrame implements ChatGui {
	private ChatClientAgent myAgent;
	private TextField writeTf;
	private TextArea allTa;
	private ParticipantsFrame participantsFrame;
	private MobileAgentGui mobileFrame;
	private Button b_1;
	
	public AWTChatGui(ChatClientAgent a) {
		myAgent = a;
		MobileAgent mobileAgent = new MobileAgent();
		
		setTitle("Chat: "+myAgent.getLocalName());
		setSize(new Dimension(286, 284));
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
		
		mobileFrame = new MobileAgentGui (mobileAgent);
		Button b2 = new Button("Mover");
		b2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!mobileFrame.isVisible()) {
					mobileFrame.setVisible(true);
				}	
			}
		});
		b2.setActionCommand("Mover");
		b2.setBounds(138, 205, 132, 40);
		getContentPane().add(b2);
		
		participantsFrame = new ParticipantsFrame(this, myAgent.getLocalName());
		
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		show();
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
	
	public void dispose() {
		participantsFrame.dispose();
		super.dispose();
	}
}



